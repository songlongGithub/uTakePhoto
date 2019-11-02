package com.sl.utakephoto.compress;


import android.net.Uri;


/**
 * 压缩配置类
 */
public class CompressConfig {
    /**
     * 压缩后输入Uri
     */
    private Uri targetUri;
    /**
     * 是否支持透明度
     */
    private boolean focusAlpha;

    private int leastCompressSize = 100;

    private CompressConfig() {
    }


    public Uri getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(Uri targetUri) {
        this.targetUri = targetUri;
    }

    public boolean isFocusAlpha() {
        return focusAlpha;
    }

    public void setFocusAlpha(boolean focusAlpha) {
        this.focusAlpha = focusAlpha;
    }

    public int getLeastCompressSize() {
        return leastCompressSize;
    }

    public void setLeastCompressSize(int leastCompressSize) {
        this.leastCompressSize = leastCompressSize;
    }

    public static class Builder {
        private CompressConfig config;

        public Builder() {
            config = new CompressConfig();
        }

        public Builder setTargetUri(Uri targetUri) {
            config.setTargetUri(targetUri);
            return this;
        }

        public Builder setFocusAlpha(boolean focusAlpha) {
            config.setFocusAlpha(focusAlpha);
            return this;
        }

        public Builder setLeastCompressSize(int leastCompressSize) {
            config.setLeastCompressSize(leastCompressSize);
            return this;
        }

        public CompressConfig create() {
            return config;
        }
    }
}

