package com.freja.cardapplet;

public class CommandCodes {
    //  code of INS byte in the command APDU header
    public final static byte CMD_GEN_KEY_PAIR = (byte)0x43;
    public final static byte CMD_FETCH_PUBLIC_KEY = (byte)0x45;
    public final static byte CMD_SIGN = (byte)0x44;
    public final static byte CMD_BAD = (byte)0xFE;
}
