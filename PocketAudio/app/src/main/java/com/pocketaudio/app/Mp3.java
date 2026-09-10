package com.pocketaudio.app;
final class Mp3 implements AutoCloseable {
    static { System.loadLibrary("pocketmp3"); }
    private long handle;
    Mp3(int rate, int channels, int bitrate) { handle = init(rate, channels, bitrate); if(handle == 0) throw new IllegalStateException("Cannot start MP3 encoder."); }
    int encode(short[] pcm, int frames, byte[] out) { int n = encodeNative(handle, pcm, frames, out); if(n < 0) throw new IllegalStateException("MP3 encoding failed."); return n; }
    int finish(byte[] out) { int n = flush(handle, out); if(n < 0) throw new IllegalStateException("MP3 finalization failed."); return n; }
    public void close() { if(handle != 0) { destroy(handle); handle=0; } }
    private static native long init(int rate, int channels, int bitrate);
    private static native int encodeNative(long h, short[] pcm, int frames, byte[] out);
    private static native int flush(long h, byte[] out);
    private static native void destroy(long h);
}
