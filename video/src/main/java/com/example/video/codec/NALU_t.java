package com.example.video.codec;

// NALU结构
public class NALU_t {

    int startcodeprefix_len; // ! 4 for parameter sets and first slice in picture, 3 for everything else (suggested)

    int len; // ! Length of the NAL unit (Excluding the start code, which does not belong to the NALU)

    int max_size; // ! Nal Unit Buffer size

    int forbidden_bit; // ! should be always FALSE

    int nal_reference_idc; // ! NALU_PRIORITY_xxxx

    int nal_unit_type; // ! NALU_TYPE_xxxx

    byte[] buf = new byte[8000000]; // ! contains the first byte followed by the EBSP

    int lost_packets; // ! true, if packet loss is detected

}
