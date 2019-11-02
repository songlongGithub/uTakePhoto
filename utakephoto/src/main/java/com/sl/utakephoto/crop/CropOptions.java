package com.sl.utakephoto.crop;

import java.io.Serializable;

/**
 * 裁剪配置类
 */
public class CropOptions implements Serializable {
    private boolean useOwnCrop;
    private int aspectX;
    private int aspectY;
    private int outputX;
    private int outputY;

    private CropOptions() {
    }

    public int getAspectX() {
        return aspectX;
    }

    public void setAspectX(int aspectX) {
        this.aspectX = aspectX;
    }

    public int getAspectY() {
        return aspectY;
    }

    public void setAspectY(int aspectY) {
        this.aspectY = aspectY;
    }

    public int getOutputX() {
        return outputX;
    }

    public void setOutputX(int outputX) {
        this.outputX = outputX;
    }

    public int getOutputY() {
        return outputY;
    }

    public void setOutputY(int outputY) {
        this.outputY = outputY;
    }

    public boolean isUseOwnCrop() {
        return useOwnCrop;
    }

    public void setUseOwnCrop(boolean useOwnCrop) {
        this.useOwnCrop = useOwnCrop;
    }

    public static class Builder {
        private CropOptions options;

        public Builder() {
            options = new CropOptions();
        }

        public Builder setAspectX(int aspectX) {
            options.setAspectX(aspectX);
            return this;
        }

        public Builder setAspectY(int aspectY) {
            options.setAspectY(aspectY);
            return this;
        }

        public Builder setOutputX(int outputX) {
            options.setOutputX(outputX);
            return this;
        }

        public Builder setOutputY(int outputY) {
            options.setOutputY(outputY);
            return this;
        }

        public Builder setWithOwnCrop(boolean withOwnCrop) {
            options.setUseOwnCrop(withOwnCrop);
            return this;
        }

        public CropOptions create() {
            return options;
        }
    }
}
