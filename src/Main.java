import mpi.*;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        MPI.Init(args);

        int width = 1024;
        int height = 1024;
        int nPoints = 1000;
        int seed = 234;


        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Rank: " + me);

        double[][] temperature = new double[width][height];
        if (me==0){
            Random random = new Random(seed);
            for (int i = 0; i < nPoints; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                temperature[x][y] = 1;
            }
            System.out.println("Matrix");
        }
        
        int chunkSize = width / (size-1);
        double[] sendBuffer = new double[chunkSize * height];
        double[] receivedBuffer = new double[chunkSize*height];

        //chunk distribution-breaking temperature array
        if (me == 0) {
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
            System.out.println("Data distributed");
        }else {
            //receiving a message from all non zero rank processes to store data in receivedBuffer
            MPI.COMM_WORLD.Recv(receivedBuffer, 0, chunkSize*height, MPI.DOUBLE, 0,0);
            System.out.println("received data chunk");
        }
        

        MPI.Finalize();
    }
}
