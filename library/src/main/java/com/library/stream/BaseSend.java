package com.library.stream;

/**
 * Created by android1 on 2017/9/27.
 */

public abstract class BaseSend {
    protected UdpControlInterface udpControl = null;

    public abstract void starsend();

    public abstract void stopsend();

    public abstract void destroy();

    public abstract void addVideo(byte[] video);

    public abstract void addVoice(byte[] voice);

    public void setUdpControl(UdpControlInterface udpControl) {
        this.udpControl = udpControl;
    }
}
