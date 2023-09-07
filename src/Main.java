import mpi.*;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        MPI.Init(args);

        int width = 1024;
        int height = 1024;
        int nPoints = 1000;
        int seed = 234;

        long startTime = System.currentTimeMillis();

        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        int chunkSize = width / (size-1);
        double[] sendBuffer = new double[chunkSize * height];
        double[] receivedBuffer = new double[chunkSize*height];
        double[][] chunk = new double[chunkSize][height];

        double[][] temperature = new double[width][height];
        if (me==0){
            Random random = new Random(seed);
            for (int i = 0; i < nPoints; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                temperature[x][y] = 1;
            }


        //chunk distribution-breaking temperature array
            for (int rank = 1; rank<size ; rank++) { //all other workers
                int startRow = (rank-1) * chunkSize;

                int index = 0;
                for (int i = 0; i < chunkSize; i++) {
                    for (int j = 0; j < height; j++) {
                        sendBuffer[index++] = temperature[startRow + i][j]; //fill 1D array
                    }

                }
                MPI.COMM_WORLD.Send(sendBuffer, 0, chunkSize*height, MPI.DOUBLE, rank, 0);
            }
        }else {
            //receiving a message from all non zero rank processes to store data in receivedBuffer
            MPI.COMM_WORLD.Recv(receivedBuffer, 0, chunkSize*height, MPI.DOUBLE, 0,0);

            int index=0;
            for (int i = 0; i < chunkSize; ++i) {
                for (int j = 0; j < height; ++j) {
                    chunk[i][j] = receivedBuffer[index++];
                }
            }

            boolean localStable = false;
            while (!localStable) {

                localStable = true;

                for (int i = 0; i < chunkSize; ++i) {
                    for (int j = 0; j < height; ++j) {
                        if (temperature[i][j] == 1) continue;

                        double value = 0;
                        if (i > 0) value += temperature[i - 1][j];
                        if (i < chunk[0].length - 1) value += temperature[i + 1][j];
                        if (j > 0) value += temperature[i][j - 1];
                        if (j < chunk.length - 1) value += temperature[i][j + 1];
                        value /= 4;

                        double diff = Math.abs(value - temperature[i][j]);
                        if (diff > 0.0025) {
                            localStable = false;
                        }
                        temperature[i][j] = value;
                    }
                }
            }

            index = 0;
            for (int i = 0; i < chunkSize; ++i) {
                for (int j = 0; j < height; ++j) {
                    sendBuffer[index++] = chunk[i][j];
                }
            }

            //updated chunk back to rank 0
            MPI.COMM_WORLD.Send(sendBuffer, 0, chunkSize * height, MPI.DOUBLE, 0, 0);
        }

        if (me == 0) {
            //merging updated chunks
            for (int rank = 1; rank < size; ++rank) {
                int startRow = (rank - 1) * chunkSize;
                MPI.COMM_WORLD.Recv(receivedBuffer, 0, chunkSize * height, MPI.DOUBLE, rank, 0);

                int index = 0;
                for (int i = 0; i < chunkSize; ++i) {
                    for (int j = 0; j < height; ++j) {
                        temperature[startRow + i][j] = receivedBuffer[index++];
                    }
                }
            }
            System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + " ms");
        }

        MPI.Finalize();
    }
}
