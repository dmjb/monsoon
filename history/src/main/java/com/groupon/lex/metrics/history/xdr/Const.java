/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupon.lex.metrics.history.xdr;

import java.io.IOException;
import java.util.Arrays;
import org.dcache.xdr.OncRpcException;
import org.dcache.xdr.XdrDecodingStream;
import org.dcache.xdr.XdrEncodingStream;

/**
 *
 * @author ariane
 */
public class Const {
    private Const() {}

    public static byte[] MAGIC = new byte[]{  17,  19,  23,  29,
                                             'M', 'O', 'N', '-',
                                             's', 'o', 'o', 'n' };  // 12 chars
    public static short MAJOR = 1;
    public static short MINOR = 0;

    public static int version_from_majmin(short maj, short min) {
        if (maj < 0 || min < 0) throw new IllegalArgumentException("Java needs unsigned data types!");
        return (int)maj << 16 | (int)min;
    }

    public static short version_major(int ver) {
        if (ver < 0) throw new IllegalArgumentException("Java needs unsigned data types!");
        return (short)(ver >> 16);
    }

    public static short version_minor(int ver) {
        if (ver < 0) throw new IllegalArgumentException("Java needs unsigned data types!");
        return (short)(ver & 0xffff);
    }

    public static enum Validation {
        /** File is written with older major version. */
        OLD_MAJOR(-1, -1),
        /** File is written with older minor version, but same major version. */
        OLD_MINOR(0, -1),
        /** File is written with current version. */
        CURRENT(0, 0),
        /** File is written with newer minor version. */
        NEW_MINOR(0, 1),
        /** File is written with newer major version. */
        NEW_MAJOR(1, 1),
        /** File is not a valid tsdata file. */
        INVALID(Integer.MAX_VALUE, Integer.MAX_VALUE);

        private final int same_major_;
        private final int same_minor_;

        private Validation(int same_major, int same_minor) {
            same_major_ = same_major;
            same_minor_ = same_minor;
        }

        public boolean isSameMajor() { return same_major_ == 0; }
        public boolean isSameMinor() { return same_minor_ == 0; }
        public boolean isAcceptable() { return isSameMajor() && same_minor_ <= 0; }
        public boolean isReadable() { return same_major_ < 0 || isAcceptable(); }
    }

    public static boolean isUpgradable(short maj, short min) {
        return (maj == MAJOR && min <= MINOR);
    }

    public static boolean isUpgradable(int version) {
        return isUpgradable(version_major(version), version_minor(version));
    }

    public static boolean needsUpgrade(short maj, short min) {
        return isUpgradable(maj, min) && (maj != MAJOR || min != MINOR);
    }

    public static boolean needsUpgrade(int version) {
        return needsUpgrade(version_major(version), version_minor(version));
    }

    public static Validation validateHeader(tsfile_mimeheader hdr) {
        if (!Arrays.equals(MAGIC, hdr.magic)) return Validation.INVALID;
        if (hdr.version_number < 0) return Validation.INVALID;
        int maj_cmp = Short.compare(version_major(hdr.version_number), MAJOR);
        int min_cmp = Short.compare(version_minor(hdr.version_number), MINOR);
        if (maj_cmp != 0) return (maj_cmp < 0 ? Validation.OLD_MAJOR : Validation.NEW_MAJOR);
        if (min_cmp != 0) return (min_cmp < 0 ? Validation.OLD_MINOR : Validation.NEW_MINOR);
        return Validation.CURRENT;
    }

    public static int validateHeaderOrThrow(tsfile_mimeheader hdr) throws IOException {
        if (!validateHeader(hdr).isReadable())
            throw new IOException("Can't read this file, header validation yields " + validateHeader(hdr).name());
        return hdr.version_number;
    }

    public static boolean validateHeaderOrThrowForWrite(tsfile_mimeheader hdr) throws IOException {
        if (!validateHeader(hdr).isAcceptable() || !isUpgradable(hdr.version_number))
            throw new IOException("Can't read this file, header validation yields " + validateHeader(hdr).name());
        return needsUpgrade(hdr.version_number);
    }

    public static int validateHeaderOrThrow(XdrDecodingStream decoder) throws IOException, OncRpcException {
        return validateHeaderOrThrow(new tsfile_mimeheader(decoder));
    }

    public static boolean validateHeaderOrThrowForWrite(XdrDecodingStream decoder) throws IOException, OncRpcException {
        return validateHeaderOrThrowForWrite(new tsfile_mimeheader(decoder));
    }

    public static void writeMimeHeader(XdrEncodingStream encoder) throws IOException, OncRpcException {
        tsfile_mimeheader hdr = new tsfile_mimeheader();
        hdr.magic = MAGIC;
        hdr.version_number = version_from_majmin(MAJOR, MINOR);
        hdr.xdrEncode(encoder);
    }
}
