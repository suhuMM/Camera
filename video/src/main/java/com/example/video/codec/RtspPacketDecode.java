package com.example.video.codec;

public class RtspPacketDecode {


    private byte[] h264Buffer;
    private int h264Len = 0;
    private int h264Pos = 0;
    private static final byte[] start_code = {0, 0, 0, 1};     // h264 start code

    public RtspPacketDecode(int width, int height) {
        h264Buffer = new byte[getYuvBuffer(width, height)];
    }

    public RtspPacketDecode() {
    }

    // 功能：解RTP AAC音频包，声道和采样频率必须知道。
    // 参数：1.RTP包缓冲地址 2.RTP包数据大小 3.H264输出地址 4.输出数据大小
    // 返回：true:表示一帧结束 false:帧未结束 一般AAC音频包比较小，没有分片。
    public byte[] UnpackRTPAAC(byte[] bufIn) {

        int dataLen = bufIn.length - 16 + 7;
        //保存去掉adts头的数据
        byte[] tmp = new byte[dataLen];

        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        tmp[0] = (byte) 0xFF;
        tmp[1] = (byte) 0xF9;
        tmp[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        tmp[3] = (byte) (((chanCfg & 3) << 6) + (dataLen >> 11));
        tmp[4] = (byte) ((dataLen & 0x7FF) >> 3);
        tmp[5] = (byte) (((dataLen & 7) << 5) + 0x1F);
        tmp[6] = (byte) 0xFC;

        System.arraycopy(bufIn, 16, tmp, 7, bufIn.length - 16);
        return tmp;

    }


    /**
     * RTP解包H264
     *
     * @param rtpData
     * @return
     */
    public byte[] rtp2h264(byte[] rtpData, int rtpLen) {

        int fu_header_len = 12;         // FU-Header长度为12字节
        int extension = (rtpData[0] & (1 << 4));  // X: 扩展为是否为1
        if (extension > 0) {
            // 计算扩展头的长度
            int extLen = (rtpData[12] << 24) + (rtpData[13] << 16) + (rtpData[14] << 8) + rtpData[15];
            fu_header_len += (extLen + 1) * 4;
        }
        // 解析FU-indicator
        byte indicatorType = (byte) (CalculateUtil.byteToInt(rtpData[fu_header_len]) & 0x1f); // 取出low 5 bit 则为FU-indicator type

        byte nri = (byte) ((CalculateUtil.byteToInt(rtpData[fu_header_len]) >> 5) & 0x03);    // 取出h2bit and h3bit
        byte f = (byte) (CalculateUtil.byteToInt(rtpData[fu_header_len]) >> 7);               // 取出h1bit
        byte h264_nal_header;
        byte fu_header;
        if (indicatorType == 28) {  // FU-A
            fu_header = rtpData[fu_header_len + 1];
            byte s = (byte) (rtpData[fu_header_len + 1] & 0x80);
            byte e = (byte) (rtpData[fu_header_len + 1] & 0x40);

            if (e == 64) {   // end of fu-a
                //ZOLogUtil.d("RtpParser", "end of fu-a.....;;;");
                byte[] temp = new byte[rtpLen - (fu_header_len + 2)];
                System.arraycopy(rtpData, fu_header_len + 2, temp, 0, temp.length);
                writeData2Buffer(temp, temp.length);
                if (h264Pos >= 0) {
                    h264Pos = -1;
                    if (h264Len > 0) {
                        byte[] h264Data = new byte[h264Len];
                        System.arraycopy(h264Buffer, 0, h264Data, 0, h264Len);
                        h264Len = 0;
                        return h264Data;
                    }
                }
            } else if (s == -128) { // start of fu-a
                h264Pos = 0;     // 指针归0
                writeData2Buffer(start_code, 4);        // 写入H264起始码
                h264_nal_header = (byte) ((fu_header & 0x1f) | (nri << 5) | (f << 7));
                writeData2Buffer(new byte[]{h264_nal_header}, 1);
                byte[] temp = new byte[rtpLen - (fu_header_len + 2)];
                System.arraycopy(rtpData, fu_header_len + 2, temp, 0, temp.length); // 负载数据
                writeData2Buffer(temp, temp.length);
            } else {
                byte[] temp = new byte[rtpLen - (fu_header_len + 2)];
                System.arraycopy(rtpData, fu_header_len + 2, temp, 0, temp.length);
                writeData2Buffer(temp, temp.length);
            }
        } else { // nalu
            h264Pos = 0;
            writeData2Buffer(start_code, 4);
            byte[] temp = new byte[rtpLen - fu_header_len];
            System.arraycopy(rtpData, fu_header_len, temp, 0, temp.length);
            writeData2Buffer(temp, temp.length);
            if (h264Pos >= 0) {
                h264Pos = -1;
                if (h264Len > 0) {
                    byte[] h264Data = new byte[h264Len];
                    System.arraycopy(h264Buffer, 0, h264Data, 0, h264Len);
                    h264Len = 0;
                    return h264Data;
                }
            }
        }
        return null;
    }

    private void writeData2Buffer(byte[] data, int len) {
        if (h264Pos >= 0) {
            System.arraycopy(data, 0, h264Buffer, h264Pos, len);
            h264Pos += len;
            h264Len += len;
        }
    }

    //计算h264大小
    public int getYuvBuffer(int width, int height) {
        // stride = ALIGN(width, 16)
        int stride = (int) Math.ceil(width / 16.0) * 16;
        // y_size = stride * height
        int y_size = stride * height;
        // c_stride = ALIGN(stride/2, 16)
        int c_stride = (int) Math.ceil(width / 32.0) * 16;
        // c_size = c_stride * height/2
        int c_size = c_stride * height / 2;
        // size = y_size + c_size * 2
        return y_size + c_size * 2;
    }

}
