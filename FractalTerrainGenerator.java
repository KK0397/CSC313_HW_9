import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class FractalTerrainGenerator {
    private static final int SIZE = 256;    // Grid size
    private static final float SCALE = 50f;     // Controls steepness of terrain
    private static final float ROUGHNESS = 0.5f;    // Determines fractal variation
    private static float[][] heightMap;

    public static void main(String[] args) {
        heightMap = generateHeightMap(SIZE, SCALE, ROUGHNESS);
        Scanner inputScanner = new Scanner(System.in);

        while (true) {
            System.out.println("Choose an action: (1) Elevate terrain, (2) Depress terrain, (3) Level terrain, (4) Save and exit");
            int userAction = inputScanner.nextInt();

            if (userAction == 4) {
                try {
                    saveToObjFile("fractal_terrain.obj", heightMap);
                    System.out.println("Terrain saved as fractal_terrain.obj");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            System.out.println("Enter the center X coordinate (0-" + (SIZE - 1) + "):");
            int centerX = inputScanner.nextInt();
            System.out.println("Enter the center Z coordinate (0-" + (SIZE - 1) + "):");
            int centerZ = inputScanner.nextInt();
            System.out.println("Enter the radius of the area to modify:");
            int modificationRadius = inputScanner.nextInt();

            switch (userAction) {
                case 1:
                    System.out.println("Enter the strength of the elevation (e.g., 1.0 for small, 10.0 for large):");
                    float elevationStrength = inputScanner.nextFloat();
                    elevateTerrain(centerX, centerZ, modificationRadius, elevationStrength);
                    break;
                case 2:
                    System.out.println("Enter the strength of the depression (e.g., 1.0 for small, 10.0 for large):");
                    float depressionStrength = inputScanner.nextFloat();
                    depressTerrain(centerX, centerZ, modificationRadius, depressionStrength);
                    break;
                case 3:
                    levelTerrain(centerX, centerZ, modificationRadius);
                    break;
                default:
                    System.out.println("Invalid action.");
            }
        }

        inputScanner.close();
    }

    private static void elevateTerrain(int x, int z, int radius, float strength) {
        adjustTerrain(x, z, radius, 0.1f * strength);
    }

    private static void depressTerrain(int x, int z, int radius, float strength) {
        adjustTerrain(x, z, radius, -0.1f * strength);
    }

    private static void levelTerrain(int x, int z, int radius) {
        float targetHeight = heightMap[x][z];
        for (int i = Math.max(0, x - radius); i < Math.min(SIZE, x + radius); i++) {
            for (int j = Math.max(0, z - radius); j < Math.min(SIZE, z + radius); j++) {
                float distance = (float) Math.sqrt((i - x) * (i - x) + (j - z) * (j - z));
                if (distance <= radius) {
                    heightMap[i][j] = targetHeight;
                }
            }
        }
    }

    private static void adjustTerrain(int x, int z, int radius, float amount) {
        for (int i = Math.max(0, x - radius); i < Math.min(SIZE, x + radius); i++) {
            for (int j = Math.max(0, z - radius); j < Math.min(SIZE, z + radius); j++) {
                float distance = (float) Math.sqrt((i - x) * (i - x) + (j - z) * (j - z));
                if (distance <= radius) {
                    float falloff = 1 - (distance / radius);
                    heightMap[i][j] += amount * falloff;
                }
            }
        }
    }

    // Generates a fractal heightmap using Simplex noise
    private static float[][] generateHeightMap(int size, float scale, float roughness) {
        float[][] heightMap = new float[size][size];
        Random random = new Random();
        SimplexNoise noise = new SimplexNoise();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float nx = x / (float) size - 0.5f;
                float nz = z / (float) size - 0.5f;
                heightMap[x][z] = noise.noise(nx * scale, nz * scale) * roughness;
            }
        }
        return heightMap;
    }

    private static float[][][] calculateNormals(float[][] heightMap) {
        int size = heightMap.length;
        float[][][] normals = new float[size][size][3];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                // Compute vectors using neighboring heights
                float[] normal = new float[3];

                // Get height differences using surrounding points (with boundary checks)
                float heightL = (x > 0) ? heightMap[x - 1][z] : heightMap[x][z];
                float heightR = (x < size - 1) ? heightMap[x + 1][z] : heightMap[x][z];
                float heightD = (z > 0) ? heightMap[x][z - 1] : heightMap[x][z];
                float heightU = (z < size - 1) ? heightMap[x][z + 1] : heightMap[x][z];

                // Calculate gradient vectors
                float[] vector1 = {1.0f, heightR - heightL, 0.0f};  // Vector in the X direction
                float[] vector2 = {0.0f, heightU - heightD, 1.0f};  // Vector in the Z direction

                // Compute cross product to get the normal
                normal[0] = vector1[1] * vector2[2] - vector1[2] * vector2[1];  // X component
                normal[1] = vector1[2] * vector2[0] - vector1[0] * vector2[2];  // Y component
                normal[2] = vector1[0] * vector2[1] - vector1[1] * vector2[0];  // Z component

                // Normalize the normal
                float length = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);

                if (length != 0) {
                    normal[0] /= length;
                    normal[1] /= length;
                    normal[2] /= length;
                }

                // Store the normal
                normals[x][z] = normal;
            }
        }
        return normals;
    }

    // Saves the generated heightmap into a Wavefront .obj file with normals
    private static void saveToObjFile(String fileName, float[][] heightMap) throws IOException {
        int size = heightMap.length;
        float[][][] normals = calculateNormals(heightMap);  // Calculate normals

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Writing vertices
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    writer.write("v " + x + " " + heightMap[x][z] + " " + z + "\n");
                }
            }

            // Writing normals
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    float[] normal = normals[x][z];
                    writer.write("vn " + normal[0] + " " + normal[1] + " " + normal[2] + "\n");
                }
            }

            // Writing faces (triangles)
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size - 1; x++) {
                    int topLeft = (z * size) + x + 1;
                    int topRight = topLeft + 1;
                    int bottomLeft = topLeft + size;
                    int bottomRight = bottomLeft + 1;

                    // writing faces with normals
                    writer.write("f " + topLeft + "//" + topLeft + " " + bottomLeft + "//" + bottomLeft + " " + topRight + "//" + topRight + "\n");
                    writer.write("f " + topRight + "//" + topRight + " " + bottomLeft + "//" + bottomLeft + " " + bottomRight + "//" + bottomRight + "\n");
                }
            }
        }
    }

    static class SimplexNoise {
        private static final int GRADIENT_SIZE_TABLE = 256;
        private static final float[] GRADIENTS_2D = new float[GRADIENT_SIZE_TABLE * 2];
        private static final short[] PERM = new short[GRADIENT_SIZE_TABLE * 2];
        private static final float SQRT3 = 1.7320508075688772f;
        private static final float F2 = 0.5f * (SQRT3 - 1.0f);
        private static final float G2 = (3.0f - SQRT3) / 6.0f;

        // Gradient table initialization
        static {
            for (int i = 0; i < GRADIENT_SIZE_TABLE; i++) {
                double angle = Math.PI * 2 * i / GRADIENT_SIZE_TABLE;
                GRADIENTS_2D[i * 2] = (float) Math.cos(angle);
                GRADIENTS_2D[i * 2 + 1] = (float) Math.sin(angle);
            }

            for (int i = 0; i < GRADIENT_SIZE_TABLE; i++) {
                PERM[i] = (short) i;
            }

            // Shuffle permutation table
            for (int i = 0; i < GRADIENT_SIZE_TABLE; i++) {
                int j = (int) (Math.random() * GRADIENT_SIZE_TABLE);
                short temp = PERM[i];
                PERM[i] = PERM[j];
                PERM[j] = temp;
            }

            // Duplicate the permutation table
            for (int i = 0; i < GRADIENT_SIZE_TABLE; i++) {
                PERM[GRADIENT_SIZE_TABLE + i] = PERM[i];
            }
        }

        // Main 2D Simplex noise function
        public float noise(float xin, float yin) {
            float s = (xin + yin) * F2;     // Skew factor for 2D
            int i = fastFloor(xin + s);
            int j = fastFloor(yin + s);
            float t = (i + j) * G2;     // Unskew factor for 2D
            float X0 = i - t;   // Unskewed x-coordinate of grid origin
            float Y0 = j - t;   // Unskewed y-coordinate of grid origin
            float x0 = xin - X0;    // Distance from origin in x
            float y0 = yin - Y0;    // Distance from origin in y

            // Determine which simplex we are in
            int i1, j1;     // Offsets for second corner of simplex
            if (x0 > y0) {
                i1 = 1;
                j1 = 0;
            } else {
                i1 = 0;
                j1 = 1;
            }

            // Second corner's coordinates
            float x1 = x0 - i1 + G2;
            float y1 = y0 - j1 + G2;

            // Third corner's coordinates
            float x2 = x0 - 1.0f + 2.0f * G2;
            float y2 = y0 - 1.0f + 2.0f * G2;

            // Calculate the hashed gradient indices
            int ii = i & 255;
            int jj = j & 255;
            int gi0 = PERM[ii + PERM[jj]] % GRADIENT_SIZE_TABLE;
            int gi1 = PERM[ii + i1 + PERM[jj + j1]] % GRADIENT_SIZE_TABLE;
            int gi2 = PERM[ii + 1 + PERM[jj + 1]] % GRADIENT_SIZE_TABLE;

            // Calculate contributions from each corner
            float n0, n1, n2;   // Noise contributions from the three corners

            // First corner contribution
            float t0 = 0.5f - x0 * x0 - y0 * y0;
            if (t0 < 0) {
                n0 = 0.0f;
            } else {
                t0 *= t0;
                n0 = t0 * t0 * dot(gi0, x0, y0);
            }

            // Second corner contribution
            float t1 = 0.5f - x1 * x1 - y1 * y1;
            if (t1 < 0) {
                n1 = 0.0f;
            } else {
                t1 *= t1;
                n1 = t1 * t1 * dot(gi1, x1, y1);
            }

            // Third corner contribution
            float t2 = 0.5f - x2 * x2 - y2 * y2;
            if (t2 < 0) {
                n2 = 0.0f;
            } else {
                t2 *= t2;
                n2 = t2 * t2 * dot(gi2, x2, y2);
            }

            // Sum up the contributions and return the result, scaled to [-1, 1]
            return 70.0f * (n0 + n1 + n2);
        }

        // Helper function to compute the dot product of the gradient and distance vectors
        private float dot(int g, float x, float y) {
            return GRADIENTS_2D[g * 2] * x + GRADIENTS_2D[g * 2 + 1] * y;
        }

        // Helper function to quickly floor a floating point number
        private int fastFloor(float x) {
            return x > 0 ? (int) x : (int) x - 1;
        }
    }
}