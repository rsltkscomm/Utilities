package smartUIComparison;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Basic image comparison utilities without external logging dependencies
 */
public class ImageComparisonUtils {
    
    /**
     * Compare two images and return the difference percentage
     */
    public static double compareImagesBasic(String baselinePath, String actualPath, String diffOutPath) {
        try {
            BufferedImage baselineImage = ImageIO.read(new File(baselinePath));
            BufferedImage actualImage = ImageIO.read(new File(actualPath));
            
            if (baselineImage == null || actualImage == null) {
                System.err.println("Could not load one or both images");
                return 100.0; // Maximum difference if images can't be loaded
            }
            
            // Resize images if they have different dimensions
            if (baselineImage.getWidth() != actualImage.getWidth() || 
                baselineImage.getHeight() != actualImage.getHeight()) {
                System.out.println("Images have different dimensions, resizing for comparison");
                actualImage = resizeImage(actualImage, baselineImage.getWidth(), baselineImage.getHeight());
            }
            
            double difference = calculatePixelDifference(baselineImage, actualImage);
            
            // Create difference image if requested
            if (diffOutPath != null && !diffOutPath.isEmpty()) {
                createDifferenceImage(baselineImage, actualImage, diffOutPath);
            }
            
            return difference;
            
        } catch (IOException e) {
            System.err.println("Error comparing images: " + e.getMessage());
            return 100.0; // Maximum difference on error
        }
    }
    
    /**
     * Calculate pixel-by-pixel difference between two images
     */
    private static double calculatePixelDifference(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();
        int totalPixels = width * height;
        int differentPixels = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color1 = new Color(img1.getRGB(x, y));
                Color color2 = new Color(img2.getRGB(x, y));
                
                // Calculate color difference
                int redDiff = Math.abs(color1.getRed() - color2.getRed());
                int greenDiff = Math.abs(color1.getGreen() - color2.getGreen());
                int blueDiff = Math.abs(color1.getBlue() - color2.getBlue());
                
                // Consider pixels different if any color channel differs by more than 10
                if (redDiff > 10 || greenDiff > 10 || blueDiff > 10) {
                    differentPixels++;
                }
            }
        }
        
        return (double) differentPixels / totalPixels * 100.0;
    }
    
    /**
     * Create a difference image highlighting differences between two images
     */
    private static void createDifferenceImage(BufferedImage img1, BufferedImage img2, String outputPath) {
        try {
            int width = img1.getWidth();
            int height = img1.getHeight();
            BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color color1 = new Color(img1.getRGB(x, y));
                    Color color2 = new Color(img2.getRGB(x, y));
                    
                    int redDiff = Math.abs(color1.getRed() - color2.getRed());
                    int greenDiff = Math.abs(color1.getGreen() - color2.getGreen());
                    int blueDiff = Math.abs(color1.getBlue() - color2.getBlue());
                    
                    // If pixels are different, highlight in red
                    if (redDiff > 10 || greenDiff > 10 || blueDiff > 10) {
                        diffImage.setRGB(x, y, Color.RED.getRGB());
                    } else {
                        // Show original image for same pixels
                        diffImage.setRGB(x, y, color1.getRGB());
                    }
                }
            }
            
            // Save difference image
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();
            ImageIO.write(diffImage, "PNG", outputFile);
            
        } catch (IOException e) {
            System.err.println("Error creating difference image: " + e.getMessage());
        }
    }
    
    /**
     * Resize an image to specified dimensions
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
    
    /**
     * Check if two images are identical (0% difference)
     */
    public static boolean areImagesIdentical(String image1Path, String image2Path) {
        double difference = compareImagesBasic(image1Path, image2Path, null);
        return difference < 0.1; // Allow for minor floating point differences
    }
    
    /**
     * Compare images with custom tolerance
     */
    public static boolean compareImagesWithTolerance(String baselinePath, String actualPath, 
                                                   String diffOutPath, double tolerance) {
        double difference = compareImagesBasic(baselinePath, actualPath, diffOutPath);
        return difference <= tolerance;
    }
}