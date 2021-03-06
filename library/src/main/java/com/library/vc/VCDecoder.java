package com.library.vc;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.library.file.WriteMp4;
import com.library.stream.BaseRecive;
import com.library.stream.VoiceCallback;
import com.library.util.OtherUtil;
import com.library.util.mLog;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by android1 on 2017/9/23.
 */

public class VCDecoder implements VoiceCallback {
    private final String AAC_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;

    private MediaCodec mDecoder;
    private BaseRecive baseRecive;
    private WriteMp4 writeMp4;
    private boolean isdecoder = false;
    private VoicePlayer voicePlayer;

    public VCDecoder(int samplerate, BaseRecive baseRecive, WriteMp4 writeMp4) {
        this.baseRecive = baseRecive;
        baseRecive.setVoiceCallback(this);
        this.writeMp4 = writeMp4;
        try {
            //需要解码数据的类型
            //初始化解码器
            mDecoder = MediaCodec.createDecoderByType(AAC_MIME);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, AAC_MIME);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, samplerate);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);

            byte[] data = new byte[]{(byte) 0x12, (byte) 0x10};
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(data));

            writeMp4.addTrack(mediaFormat, WriteMp4.voice);

            //解码器配置
            mDecoder.configure(mediaFormat, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDecoder.start();
    }

    public void register(VoicePlayer voicePlayer) {
        this.voicePlayer = voicePlayer;
    }

    @Override
    public void voiceCallback(byte[] voice) {
        if (isdecoder) {
            //写文件
            writeFile(voice, voice.length);
            //音频解码耗时较少，直接单线程顺序执行解码
            decoder(voice);
        }
    }

    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private int outputBufferIndex;

    public void decoder(byte[] voice) {
        try {
            //返回一个包含有效数据的input buffer的index,-1->不存在
            int inputBufIndex = mDecoder.dequeueInputBuffer(OtherUtil.waitTime);
            if (inputBufIndex >= 0) {
                //获取当前的ByteBuffer
                ByteBuffer dstBuf = mDecoder.getInputBuffer(inputBufIndex);
                dstBuf.clear();
                dstBuf.put(voice, 0, voice.length);
                mDecoder.queueInputBuffer(inputBufIndex, 0, voice.length, 0, 0);
            } else {
                mLog.log("dcoder_failure", "dcoder failure_VC");
                return;
            }
            outputBufferIndex = mDecoder.dequeueOutputBuffer(info, OtherUtil.waitTime);

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mDecoder.getOutputBuffer(outputBufferIndex);
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                outputBuffer.clear();
                if (voicePlayer != null) {
                    //通过接口回调播放
                    voicePlayer.voicePlayer(outData);
                }
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, OtherUtil.waitTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void star() {
        isdecoder = true;
    }


    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer writebuffer = ByteBuffer.allocate(548);

    /*
    写入文件
     */
    private void writeFile(byte[] output, int length) {
        writebuffer.clear();
        writebuffer.put(output);
        bufferInfo.size = length;
        bufferInfo.offset = 0;
        bufferInfo.presentationTimeUs = OtherUtil.getFPS();
        bufferInfo.flags = MediaCodec.CRYPTO_MODE_UNENCRYPTED;
        writeMp4.write(WriteMp4.voice, writebuffer, bufferInfo);
    }

    /*
     * 释放资源
     */
    public void stop() {
        isdecoder = false;
    }

    public void destroy() {
        isdecoder = false;
        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;
    }
}

