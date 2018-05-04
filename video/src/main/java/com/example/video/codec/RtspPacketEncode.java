package com.example.video.codec;


import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class RtspPacketEncode {

    private static final String TAG = "RtspPacketEncode";


    //------------视频转换数据监听-----------
    public interface H264ToRtpLinsener {
        void h264ToRtpResponse(byte[] out, int len);
    }

    private H264ToRtpLinsener h264ToRtpLinsener;

    //执行回调
    private void exceuteH264ToRtpLinsener(byte[] out, int len) {
        if (this.h264ToRtpLinsener != null) {
            h264ToRtpLinsener.h264ToRtpResponse(out, len);
        }
    }

    //------------------音频转换监听-------------------------

    public interface AccToRtpLinsener {
        void accToRtpResponse(byte[] out, int len);
    }

    private AccToRtpLinsener accToRtpLinsener;

    //执行回调
    private void exceuteAacToRtpLinsener(byte[] out, int len) {
        if (this.accToRtpLinsener != null) {
            accToRtpLinsener.accToRtpResponse(out, len);
        }
    }


    // -------音频-------
    // 时间戳增量
    private float audio_sample_rate = 44100;
    private int audioTimestamp_increse = 2014;
    ;  // +0.5)(80000.0 / audioFramerate * 1000 +750);(1024 * 100000 / audioFramerate + 14); (int) (90000.0 * dataLen / audio_sample_rate);(93330.0 / audio_sample_rate * 1000);
    private int audioTs_current = 0;
    ;
    private int audioSeq_num = 0;
    private int audioLen = 0;

    // -------音频END-------

    // -------视频--------
    private int framerate = 10;
    private byte[] sendbuf = new byte[1500];
    private int packageSize = 1400;
    private int seq_num = 0;
    private int timestamp_increse = (int) (90000.0 / framerate);//framerate是帧率
    private int ts_current = 0;
    private int bytes = 0;

    // -------视频END--------

    public RtspPacketEncode(H264ToRtpLinsener h264ToRtpLinsener) {
        this.h264ToRtpLinsener = h264ToRtpLinsener;
    }

    public RtspPacketEncode(AccToRtpLinsener accToRtpLinsener) {
        this.accToRtpLinsener = accToRtpLinsener;
    }


    public void AacToRtp(byte[] aac) {

        Log.v(TAG, "--->>" + aac.length);
        int dataLen = aac.length - 7;
        //保存去掉adts头的数据
        byte[] tmp = new byte[dataLen];
        System.arraycopy(aac, 7, tmp, 0, dataLen);

        //新数据的包长度
        byte[] rtp = new byte[4 + 12 + dataLen];

        //CalculateUtil.memset(rtp, 0, 4 + 12 + dataLen);

        rtp[1] = (byte) (rtp[1] | 96); // 负载类型号96,其值为：01100000
        rtp[0] = (byte) (rtp[0] | 0x80); // 版本号,此版本固定为2
        rtp[1] = (byte) (rtp[1] & 254); //标志位，由具体协议规定其值，其值为：01100000
        rtp[11] = 10;//随机指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换（同源标识符的最后一个字节）
        rtp[1] = (byte) (rtp[1] | 0x80); // 设置rtp M位为1，其值为：11100000，分包的最后一片，M位（第一位）为0，后7位是十进制的96，表示负载类型
        rtp[3] = (byte) audioSeq_num++;
        System.arraycopy(CalculateUtil.intToByte(audioSeq_num++), 0, rtp, 2, 2);//send[2]和send[3]为序列号，共两位
        {
            // java默认的网络字节序是大端字节序（无论在什么平台上），因为windows为小字节序，所以必须倒序
            /**参考：
             * http://blog.csdn.net/u011068702/article/details/51857557
             * http://cpjsjxy.iteye.com/blog/1591261
             */
            byte temp = 0;
            temp = rtp[3];
            rtp[3] = rtp[2];
            rtp[2] = temp;
        }

        //加上4个字节的au_header + au_header_size
        rtp[12] = (byte) 0x00;
        rtp[13] = (byte) 0x10;
        rtp[14] = (byte) ((dataLen & 0x1FE0) >> 5);
        rtp[15] = (byte) ((dataLen & 0x001F) << 3);

        //NALU头已经写到sendbuf[12]中，接下来则存放的是NAL的第一个字节之后的数据。所以从r的第二个字节开始复制
        System.arraycopy(tmp, 0, rtp, 16, dataLen);
        //audioTs_current = audioTs_current + audioTimestamp_increse;((96000 * dataLen / 2048) / (2048 * 1000 / 44100));(96000 * dataLen / 44100);
        //double t = 96000 * dataLen / 44100;
        // audioTs_current = (int) (audioTs_current + t);
        //audioTs_current =(int) SystemClock.currentThreadTimeMillis();
        System.arraycopy(CalculateUtil.intToByte(audioTs_current), 0, rtp, 4, 4);//序列号接下来是时间戳，4个字节，存储后也需要倒序
        {
            byte temp = 0;
            temp = rtp[4];
            rtp[4] = rtp[7];
            rtp[7] = temp;
            temp = rtp[5];
            rtp[5] = rtp[6];
            rtp[6] = temp;
        }
        audioLen = 4 + 12 + dataLen;//获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
        //client.send(new DatagramPacket(sendbuf, bytes, addr, port/*9200*/));
        //send(sendbuf,bytes);

        exceuteAacToRtpLinsener(rtp, audioLen);
    }


    /**
     * 一帧一帧的RTP封包
     *
     * @param r
     * @return
     */
    public void h264ToRtp(byte[] r, int h264len) throws Exception {

        CalculateUtil.memset(sendbuf, 0, 1500);
        sendbuf[1] = (byte) (sendbuf[1] | 96); // 负载类型号96,其值为：01100000
        sendbuf[0] = (byte) (sendbuf[0] | 0x80); // 版本号,此版本固定为2
        sendbuf[1] = (byte) (sendbuf[1] & 254); //标志位，由具体协议规定其值，其值为：01100000
        sendbuf[11] = 10;//随机指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换（同源标识符的最后一个字节）
        if (h264len <= packageSize) {
            sendbuf[1] = (byte) (sendbuf[1] | 0x80); // 设置rtp M位为1，其值为：11100000，分包的最后一片，M位（第一位）为0，后7位是十进制的96，表示负载类型
            sendbuf[3] = (byte) seq_num++;
            System.arraycopy(CalculateUtil.intToByte(seq_num++), 0, sendbuf, 2, 2);//send[2]和send[3]为序列号，共两位
            {
                // java默认的网络字节序是大端字节序（无论在什么平台上），因为windows为小字节序，所以必须倒序
                /**参考：
                 * http://blog.csdn.net/u011068702/article/details/51857557
                 * http://cpjsjxy.iteye.com/blog/1591261
                 */
                byte temp = 0;
                temp = sendbuf[3];
                sendbuf[3] = sendbuf[2];
                sendbuf[2] = temp;
            }
            // FU-A HEADER, 并将这个HEADER填入sendbuf[12]
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x1f)));
            // 同理将sendbuf[13]赋给nalu_payload
            //NALU头已经写到sendbuf[12]中，接下来则存放的是NAL的第一个字节之后的数据。所以从r的第二个字节开始复制
            System.arraycopy(r, 1, sendbuf, 13, h264len - 1);
            ts_current = ts_current + timestamp_increse;
            System.arraycopy(CalculateUtil.intToByte(ts_current), 0, sendbuf, 4, 4);//序列号接下来是时间戳，4个字节，存储后也需要倒序
            {
                byte temp = 0;
                temp = sendbuf[4];
                sendbuf[4] = sendbuf[7];
                sendbuf[7] = temp;
                temp = sendbuf[5];
                sendbuf[5] = sendbuf[6];
                sendbuf[6] = temp;
            }
            bytes = h264len + 12;//获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
            //client.send(new DatagramPacket(sendbuf, bytes, addr, port/*9200*/));
            //send(sendbuf,bytes);
            exceuteH264ToRtpLinsener(sendbuf, bytes);

        } else if (h264len > packageSize) {
            int k = 0, l = 0;
            k = h264len / packageSize;
            l = h264len % packageSize;
            int t = 0;
            ts_current = ts_current + timestamp_increse;
            System.arraycopy(CalculateUtil.intToByte(ts_current), 0, sendbuf, 4, 4);//时间戳，并且倒序
            {
                byte temp = 0;
                temp = sendbuf[4];
                sendbuf[4] = sendbuf[7];
                sendbuf[7] = temp;
                temp = sendbuf[5];
                sendbuf[5] = sendbuf[6];
                sendbuf[6] = temp;
            }
            while (t <= k) {
                System.arraycopy(CalculateUtil.intToByte(seq_num++), 0, sendbuf, 2, 2);//序列号，并且倒序
                {
                    byte temp = 0;
                    temp = sendbuf[3];
                    sendbuf[3] = sendbuf[2];
                    sendbuf[2] = temp;
                }
                if (t == 0) {//分包的第一片
                    sendbuf[1] = (byte) (sendbuf[1] & 0x7F);//其值为：01100000，不是最后一片，M位（第一位）设为0
                    //FU indicator，一个字节，紧接在RTP header之后，包括F,NRI，header
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);//禁止位，为0
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);//NRI，表示包的重要性
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));//TYPE，表示此FU-A包为什么类型，一般此处为28
                    //FU header，一个字节，S,E，R，TYPE
                    sendbuf[13] = (byte) (sendbuf[13] & 0xBF);//E=0，表示是否为最后一个包，是则为1
                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF);//R=0，保留位，必须设置为0
                    sendbuf[13] = (byte) (sendbuf[13] | 0x80);//S=1，表示是否为第一个包，是则为1
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (r[0] & 0x1f)));//TYPE，即NALU头对应的TYPE
                    //将除去NALU头剩下的NALU数据写入sendbuf的第14个字节之后。前14个字节包括：12字节的RTP Header，FU indicator，FU header
                    System.arraycopy(r, 1, sendbuf, 14, packageSize);
                    //client.send(new DatagramPacket(sendbuf, packageSize + 14, addr, port/*9200*/));
                    exceuteH264ToRtpLinsener(sendbuf, packageSize + 14);
                    t++;
                } else if (t == k) {//分片的最后一片
                    sendbuf[1] = (byte) (sendbuf[1] | 0x80);

                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0，保留位必须设为0
                    sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0，不是第一个包
                    sendbuf[13] = (byte) (sendbuf[13] | 0x40); //E=1，是最后一个包
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (r[0] & 0x1f)));//NALU头对应的type

                    if (0 != l) {//如果不能整除，则有剩下的包，执行此代码。如果包大小恰好是1400的倍数，不执行此代码。
                        System.arraycopy(r, t * packageSize + 1, sendbuf, 14, l - 1);//l-1，不包含NALU头
                        bytes = l - 1 + 14; //bytes=l-1+14;
                        //client.send(new DatagramPacket(sendbuf, bytes, addr, port/*9200*/));
                        //send(sendbuf,bytes);
                        exceuteH264ToRtpLinsener(sendbuf, bytes);
                    }//pl
                    t++;
                } else if (t < k && 0 != t) {//既不是第一片，又不是最后一片的包
                    sendbuf[1] = (byte) (sendbuf[1] & 0x7F); //M=0，其值为：01100000，不是最后一片，M位（第一位）设为0.
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (r[0] & 0x80)) << 7);
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((r[0] & 0x60) >> 5)) << 5);
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0，保留位必须设为0
                    sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0，不是第一个包
                    sendbuf[13] = (byte) (sendbuf[13] & 0xBF); //E=0，不是最后一个包
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (r[0] & 0x1f)));//NALU头对应的type
                    System.arraycopy(r, t * packageSize + 1, sendbuf, 14, packageSize);//不包含NALU头
                    //client.send(new DatagramPacket(sendbuf, packageSize + 14, addr, port/*9200*/));
                    //send(sendbuf,1414);
                    exceuteH264ToRtpLinsener(sendbuf, packageSize + 14);

                    t++;
                }
            }
        }
    }


    /**
     * 文件封包
     */
    int info2 = 0, info3 = 0;
    InputStream in;

    public void h264ToRtp2(byte[] r, int h264len) throws Exception {

        in = new ByteArrayInputStream(r);

        int seq_num = 0;
        int bytes = 0;

        // 时间戳增量
        float framerate = 25;
        int timestamp_increse = 0, ts_current = 0;
        timestamp_increse = (int) (90000.0 / framerate); //+0.5);
        // rtp包缓冲
        byte[] sendbuf = new byte[1500];

        while (!(0 == in.available())) {

            NALU_t n = new NALU_t();
            GetAnnexbNALU(n); // 每执行一次, 文件指针指向本次找到的NALU的末尾, 下一位置即为下个NALU的起始码0x000001
            CalculateUtil.dump(n);//输出NALU的长度和NALU
            // 从文件中 获取一个nalu大小
            // 判断其大小 分包发送
            CalculateUtil.memset(sendbuf, 0, 1500);//情况sendbuf,此时会将上次的时间戳清空,因此需要ts_current来保存上次的时间戳值

            sendbuf[1] = (byte) (sendbuf[1] | 96); // 负载类型号96
            //System.out.println("-----!"+sendbuf[1]);
            sendbuf[0] = (byte) (sendbuf[0] | 0x80); // 版本号,此版本固定为2
            sendbuf[1] = (byte) (sendbuf[1] & 254); //标志位，由具体协议规定其值
            sendbuf[11] = 10;    //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换

            if (n.len <= 1400) {
                sendbuf[1] = (byte) (sendbuf[1] | 0x80); // 设置rtp M位为1
                //sendbug[2], sendbuf[3]赋值seq_num ++ 每发送一次rtp包增1
                //sendbuf[3] = (byte) seq_num ++
                System.arraycopy(CalculateUtil.intToByte(seq_num++), 0, sendbuf, 2, 2);
                {
                    // 倒序
                    byte temp = 0;
                    temp = sendbuf[3];
                    sendbuf[3] = sendbuf[2];
                    sendbuf[2] = temp;
                }

                // 设置NALU HEADER, 并将这个HEADER填入sendbuf[12]
                sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
                sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
                sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.nal_unit_type));
                // 同理将sendbuf[13]赋给nalu_payload
                System.arraycopy(n.buf, 1, sendbuf, 13, n.len - 1);//去掉nalu头的nalu剩余类容写入sendbuf[13]开始的字符串

                ts_current = ts_current + timestamp_increse;
                //rtp_hdr.timestamp = ts_current;// htonl(ts_current) java默认网络字节序
                System.arraycopy(CalculateUtil.intToByte(ts_current), 0, sendbuf, 4, 4);
                {
                    // 倒序
                    byte temp = 0;
                    temp = sendbuf[4];
                    sendbuf[4] = sendbuf[7];
                    sendbuf[7] = temp;

                    temp = sendbuf[5];
                    sendbuf[5] = sendbuf[6];
                    sendbuf[6] = temp;
                }
                bytes = n.len + 12;                    //获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
                // Send(sendbuf, bytes);//发送rtp包
                exceuteH264ToRtpLinsener(sendbuf, bytes);
            } else if (n.len > 1400) {
                // 得到该nalu需要用多少长度为1400字节的rtp包来发送
                int k = 0, l = 0;
                k = n.len / 1400; //需要k个1400字节的rtp包
                l = n.len % 1400; //最后一个rtp包需要装载的字节数
                int t = 0; // 用于指示当前发送的第几个分片RTP包
                ts_current = ts_current + timestamp_increse;
                //rtp_hdr->timestamp=htonl(ts_current);
                System.arraycopy(CalculateUtil.intToByte(ts_current), 0, sendbuf, 4, 4);
                {
                    // 倒序
                    byte temp = 0;
                    temp = sendbuf[4];
                    sendbuf[4] = sendbuf[7];
                    sendbuf[7] = temp;

                    temp = sendbuf[5];
                    sendbuf[5] = sendbuf[6];
                    sendbuf[6] = temp;

                }
                while (t <= k) {
                    //rtp_hdr->seq_no = htons(seq_num ++);//序列号, 每发送一个rtp包增加1
                    //sendbuf[3] = (byte) seq_num ++;
                    System.arraycopy(CalculateUtil.intToByte(seq_num++), 0, sendbuf, 2, 2);
                    {
                        // 倒序
                        byte temp = 0;
                        temp = sendbuf[3];
                        sendbuf[3] = sendbuf[2];
                        sendbuf[2] = temp;
                    }
                    if (0 == t) {
                        // 设置rtp M位
                        sendbuf[1] = (byte) (sendbuf[1] & 0x7F); // M=0
                        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
                        sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                        // 设置FU HEADER,并将这个HEADER填入snedbuf[13]
                        sendbuf[13] = (byte) (sendbuf[13] & 0xBF);//E=0
                        sendbuf[13] = (byte) (sendbuf[13] & 0xDF);//R=0
                        sendbuf[13] = (byte) (sendbuf[13] | 0x80);//S=1
                        sendbuf[13] = (byte) (sendbuf[13] | ((byte) n.nal_unit_type));

                        // 同理将sendbuf[14]赋给nalu_playload
                        System.arraycopy(n.buf, 1, sendbuf, 14, 1400);
                        bytes = 1400 + 14;
                        //Send(sendbuf, bytes);
                        exceuteH264ToRtpLinsener(sendbuf, bytes);
                        t++;
                    }
                    // 发送一个需要分片的NALU的非第一个分片，清零FU HEADER 的S位，如果该分片是该NALU的最后一个分片，置FU HEADER的E位
                    else if (k == t) //发送的是最后一个分片，注意最后一个分片的长度可能超过1400字节（当l>1386时）
                    {
                        //  设置rtp M位,当前床书的是最后一个分片时该位置1
                        sendbuf[1] = (byte) (sendbuf[1] | 0x80);
                        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
                        sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
                        sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0
                        sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0
                        sendbuf[13] = (byte) (sendbuf[13] | 0x40); //E=1
                        sendbuf[13] = (byte) (sendbuf[13] | ((byte) n.nal_unit_type));

                        // 将nalu的最后神域的l-1(去掉了一个字节的nalu头)字节类容写入sendbuf[14]开始的字符串
                        System.arraycopy(n.buf, t * 1400 + 1, sendbuf, 14, l - 1);
                        bytes = l - 1 + 14;
                        //Send(sendbuf, bytes);
                        exceuteH264ToRtpLinsener(sendbuf, bytes);
                        t++;
                    } else if (t < k && 0 != t) {
                        //设置rtp M位
                        sendbuf[1] = (byte) (sendbuf[1] & 0x7F); // M=0

                        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
                        sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
                        sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0
                        sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0
                        sendbuf[13] = (byte) (sendbuf[13] & 0xBF); //E=0
                        sendbuf[13] = (byte) (sendbuf[13] | ((byte) n.nal_unit_type));

                        System.arraycopy(n.buf, t * 1400 + 1, sendbuf, 14, 1400);//去掉起始前缀的nalu剩余内容写入sendbuf[14]开始的字符串。
                        bytes = 1400 + 14;                        //获得sendbuf的长度,为nalu的长度（除去原NALU头）加上rtp_header，fu_ind，fu_hdr的固定长度14字节
                        //Send(sendbuf, bytes);//发送rtp包
                        exceuteH264ToRtpLinsener(sendbuf, bytes);
                        t++;
                    }
                }
            }
        }
        //free nalu
    }

    // 每执行一次，文件的指针指向本次找到的NALU的末尾, 下一个位置即为下个NALU的起始码0x000001
    public int GetAnnexbNALU(NALU_t nalu) {
        nalu.startcodeprefix_len = 3;// 初始化码流序列的开始符为3个字节
        int pos = 0;
        int StartCodeFound, rewind;

        byte[] tempbytes = new byte[8000000];

        try {
            // 一次读多个字节
            int byteread = 0;

            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            if ((byteread = in.read(tempbytes, 0, 3)) != 3) {
                //System.out.println("读取："+byteread);
                return 0;
            }

            // 判断是否为0x000001
            info2 = CalculateUtil.FindStartCode2(tempbytes, 0);
            if (info2 != 1) {
                // 如果不是, 再读一个字节
                if ((byteread = in.read(tempbytes, 3, 1)) != 1) {
                    return 0;
                }
                info3 = CalculateUtil.FindStartCode3(tempbytes, 0);
                if (info3 != 1) {
                    return -1;
                } else {
                    // 如果是0x00000001,得到开始前缀4个字节
                    //pos = 4;
                    nalu.startcodeprefix_len = 4;
                    pos = 4;
                }
            } else {
                // 如果是0x000001,得到开始前缀3个字节
                nalu.startcodeprefix_len = 3;
                pos = 3;
            }

            //查找下一个开始位的标志
            StartCodeFound = 0;
            info2 = 0;
            info3 = 0;


            while (!(0 != StartCodeFound)) {

                if (0 == in.available())//判断是否到文件尾部
                {
                    nalu.len = (pos - 1) - nalu.startcodeprefix_len;
                    System.arraycopy(tempbytes, nalu.startcodeprefix_len, nalu.buf, 0, nalu.len);
                    nalu.forbidden_bit = nalu.buf[0] & 0x80; //1 bit
                    nalu.nal_reference_idc = nalu.buf[0] & 0x60; // 2 bit
                    nalu.nal_unit_type = (nalu.buf[0]) & 0x1f;// 5 bit
                    return pos - 1;
                }

                //读一个字节到tempbytes中
                if ((byteread = in.read(tempbytes, pos++, 1)) != 1) {
                    return 0;
                }
                info3 = CalculateUtil.FindStartCode3(tempbytes, pos - 4);//判断是否为0x00000001
                if (info3 != 1)
                    info2 = CalculateUtil.FindStartCode2(tempbytes, pos - 3);//判断是否为0x000001
                if (info2 == 1 || info3 == 1) {
                    StartCodeFound = 1;
                } else {
                    StartCodeFound = 0;
                }
            }


            // Here, we have found another start code (and read length of startcode bytes more than we should
            // have.  Hence, go back in the file
            rewind = (info3 == 1) ? -4 : -3;
            in.skip(rewind);//把文件指针指向前一个NALU的末尾

            // Here the Start code, the complete NALU, and the next start code is in the Buf.
            // The size of Buf is pos, pos+rewind are the number of bytes excluding the next
            // start code, and (pos+rewind)-startcodeprefix_len is the size of the NALU excluding the start code

            nalu.len = (pos + rewind) - nalu.startcodeprefix_len;
            // memcpy (nalu->buf, &Buf[nalu->startcodeprefix_len], nalu->len);//拷贝一个完整的NALU 不拷贝前缀0x000001或0x00000001
            System.arraycopy(tempbytes, nalu.startcodeprefix_len, nalu.buf, 0, nalu.len);
            nalu.forbidden_bit = nalu.buf[0] & 0x80; //1 bit
            nalu.nal_reference_idc = nalu.buf[0] & 0x60; // 2 bit
            nalu.nal_unit_type = (nalu.buf[0]) & 0x1f;// 5 bit

            //free(Buf);

            return (pos + rewind);// 返回两个 开始字符之间隔的字符数, 即包含前缀NALU的长度

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return 0;
    }


}
