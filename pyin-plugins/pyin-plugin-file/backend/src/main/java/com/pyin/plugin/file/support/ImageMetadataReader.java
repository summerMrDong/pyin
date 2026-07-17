package com.pyin.plugin.file.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ImageMetadataReader {

    private ImageMetadataReader() {
    }

    public static ImageSize read(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return new ImageSize(null, null);
            }
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException exception) {
            return new ImageSize(null, null);
        }
    }

    public record ImageSize(Integer width, Integer height) {
    }
}
