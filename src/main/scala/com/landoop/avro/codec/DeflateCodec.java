package com.landoop.avro.codec;

import org.apache.avro.file.Codec;
import org.apache.avro.file.DataFileConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Implements DEFLATE (RFC1951) compression and decompression.
 * <p>
 * Note that there is a distinction between RFC1951 (deflate)
 * and RFC1950 (zlib).  zlib adds an extra 2-byte header
 * at the front, and a 4-byte checksum at the end.  The
 * code here, by passing "true" as the "nowrap" option to
 * {@link Inflater} and {@link Deflater}, is using
 * RFC1951.
 */
class DeflateCodec extends Codec {

    private ByteArrayOutputStream outputBuffer;
    private Deflater deflater;
    private Inflater inflater;
    //currently only do 'nowrap' -- RFC 1951, not zlib
    private boolean nowrap = true;
    private int compressionLevel;

    public DeflateCodec(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    @Override
    public String getName() {
        return DataFileConstants.DEFLATE_CODEC;
    }

    @Override
    public ByteBuffer compress(ByteBuffer data) throws IOException {
        ByteArrayOutputStream baos = getOutputBuffer(data.remaining());
        DeflaterOutputStream ios = new DeflaterOutputStream(baos, getDeflater());
        writeAndClose(data, ios);
        ByteBuffer result = ByteBuffer.wrap(baos.toByteArray());
        return result;
    }

    @Override
    public ByteBuffer decompress(ByteBuffer data) throws IOException {
        ByteArrayOutputStream baos = getOutputBuffer(data.remaining());
        InflaterOutputStream ios = new InflaterOutputStream(baos, getInflater());
        writeAndClose(data, ios);
        ByteBuffer result = ByteBuffer.wrap(baos.toByteArray());
        return result;
    }

    private void writeAndClose(ByteBuffer data, OutputStream to) throws IOException {
        byte[] input = data.array();
        int offset = data.arrayOffset() + data.position();
        int length = data.remaining();
        try {
            to.write(input, offset, length);
        } finally {
            to.close();
        }
    }

    // get and initialize the inflater for use.
    private Inflater getInflater() {
        if (null == inflater) {
            inflater = new Inflater(nowrap);
        }
        inflater.reset();
        return inflater;
    }

    // get and initialize the deflater for use.
    private Deflater getDeflater() {
        if (null == deflater) {
            deflater = new Deflater(compressionLevel, nowrap);
        }
        deflater.reset();
        return deflater;
    }

    // get and initialize the output buffer for use.
    private ByteArrayOutputStream getOutputBuffer(int suggestedLength) {
        if (null == outputBuffer) {
            outputBuffer = new ByteArrayOutputStream(suggestedLength);
        }
        outputBuffer.reset();
        return outputBuffer;
    }

    @Override
    public int hashCode() {
        return nowrap ? 0 : 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || obj.getClass() != getClass())
            return false;
        DeflateCodec other = (DeflateCodec) obj;
        return (this.nowrap == other.nowrap);
    }

    @Override
    public String toString() {
        return getName() + "-" + compressionLevel;
    }
}