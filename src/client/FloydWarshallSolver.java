// Import Java's special constants ∞ and -∞ which behave 
// as you expect them to when you do arithmetic. For example,
// ∞ + ∞ = ∞, ∞ + x = ∞, -∞ + x = -∞ and ∞ + -∞ = Nan
package client;

import client.model.Cell;
import client.model.Direction;
import client.model.Map;
import client.model.World;

import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NEGATIVE_INFINITY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FloydWarshallSolver {

    private static int n_row,n_col;
    private int n;
    public boolean solved;
    public double[][] dp;
    public Integer[][] next;
    public Direction[][] next_dir;
    public int g_k,g_i,g_j;

    private static final int REACHES_NEGATIVE_CYCLE = -1;

    /**
     * As input, this class takes an adjacency matrix with edge weights between nodes,
     * where POSITIVE_INFINITY is used to indicate that two nodes are not connected.
     *
     * NOTE: Usually the diagonal of the adjacency matrix is all zeros
     * (i.e. matrix[i][i] = 0 for all i) since there is typically no cost
     * to go from a node to itself, but this may depend on your graph and
     * the problem you are trying to solve.
     */

    Map map;
    public FloydWarshallSolver(Map map){
        this.map = map;
        long t0 = System.currentTimeMillis();
        g_k = 0; g_j=0;g_i=0;
        n_row = map.getRowNum();
        n_col = map.getColumnNum();
        // Copy input matrix and setup 'next' matrix for path reconstruction.

        n= n_col*n_row;
        dp = new double[n][n];
        next = new Integer[n][n];
        next_dir = new Direction[n][n];
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++) {
                if(i!=j) {
                    dp[i][j] = POSITIVE_INFINITY;
                    next[i][j]=0;
                }else{
                    next[i][j]= j;
                }

            }
        }

        for(int i=0;i<n_row;i++){
            for(int j=0;j<n_col;j++){

                boolean notWall = !map.getCell(i,j).isWall();
                int node_i = i*n_col + j;
                int node_j=toInd(i-1,j);
                double dist = POSITIVE_INFINITY;
                if(i-1>=0){
                    if(!map.getCell(i-1,j).isWall() && notWall){
                        dist = 1.0;
                        next[node_i][node_j]=node_j;
                    }
                    dp[node_i][node_j] = dist;
                }
                node_j=toInd(i+1,j);
                if(i+1<n_row){
                    dist = POSITIVE_INFINITY;
                    if(!map.getCell(i+1,j).isWall() && notWall){
                        dist = 1.0;
                        next[node_i][node_j]=node_j;
                    }
                    dp[node_i][node_j] = dist;
                }
                node_j=toInd(i,j-1);
                if(j-1>=0){
                    dist = POSITIVE_INFINITY;
                    if(!map.getCell(i,j-1).isWall() && notWall){
                        dist = 1.0;
                        next[node_i][node_j]=node_j;
                    }
                    dp[node_i][node_j] = dist;
                }
                node_j=toInd(i,j+1);
                if(j+1<n_col){
                    dist = POSITIVE_INFINITY;
                    if(!map.getCell(i,j+1).isWall() && notWall){
                        dist = 1.0;
                        next[node_i][node_j]=node_j;
                    }
                    dp[node_i][node_j] = dist;
                }
            }
        }
//        for(int i = 0; i < n; i++) {
//            for (int j = 0; j < n; j++) {
//                if (dp[i][j] != POSITIVE_INFINITY)
//                    next[i][j] = j;
//            }
//        }

        // System.out.println("CONSTRUCTOR TIME "+(t1-t0));
        // System.out.println(" 32 -> 33 " + dp[32][33]+ " * "+ next[32][33]);
    }

    public void solve(){
        if (solved) return;


        // System.out.println("n: "+n+" g_i: "+g_i+" g_j: "+g_j+" g_k "+g_k);
        for (int k=0 ; k < n; k++) {
            for ( int i = 0; i < n; i++) {
                for ( int j = 0; j < n; j++) {
                    if (dp[i][k] + dp[k][j] < dp[i][j]) {
                        dp[i][j] = dp[i][k] + dp[k][j];
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
        //  System.out.println(" ASolve 32 -> 33 " + dp[32][33]+ " * "+ next[32][33]);

        // Identify negative cycles by propagating the value 'NEGATIVE_INFINITY'
        // to every edge that is part of or reaches into a negative cycle.
//        for (int k = 0; k < n; k++)
//            for (int i = 0; i < n; i++)
//                for (int j = 0; j < n; j++)
//                    if (dp[i][k] + dp[k][j] < dp[i][j]) {
//                        dp[i][j] = NEGATIVE_INFINITY;
//                        next[i][j] = REACHES_NEGATIVE_CYCLE;
//                    }

        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){

                int[] r1 = fromInd(i);
                int[] r2 = fromInd(next[i][j]);
                if(r2[0]>r1[0]){
                    //System.out.print("D");
                    next_dir[i][j] = Direction.DOWN;
                }else{
                    if(r2[0]<r1[0]){
                        // System.out.print("U");
                        next_dir[i][j] = Direction.UP;
                    }else{
                        // System.out.println(i+"**"+j);
                        // System.out.println(next_dir[i][j]);
                        //System.out.print("X");
                        next_dir[i][j] =   r2[1]>r1[1] ? Direction.RIGHT : Direction.LEFT;
                    }
                }
            }
        }

        solved = true;
    }

    // Executes the Floyd-Warshall algorithm.
    public void solve(int delta ) {

        if (solved) return;

        // Compute all pairs shortest paths.
        int left = delta;
        // System.out.println("n: "+n+" g_i: "+g_i+" g_j: "+g_j+" g_k "+g_k);
        for ( ; g_k < n; g_k++) {
            for ( ; g_i < n; g_i++) {
                for ( ; g_j < n; g_j++) {
                    if (dp[g_i][g_k] + dp[g_k][g_j] < dp[g_i][g_j]) {
                        dp[g_i][g_j] = dp[g_i][g_k] + dp[g_k][g_j];
                        next[g_i][g_j] = next[g_i][g_k];
                    }
                    left--;
                    // if(left%50 == 0 )System.out.println("VIP "+ "g_i: "+g_i+" g_j: "+g_j+" g_k "+g_k);
                    if(left<-5) {
                        // System.out.println("LEFT IS "+left);
                        return;
                    }
                }
            }
        }

        // Identify negative cycles by propagating the value 'NEGATIVE_INFINITY'
        // to every edge that is part of or reaches into a negative cycle.
//        for (int k = 0; k < n; k++)
//            for (int i = 0; i < n; i++)
//                for (int j = 0; j < n; j++)
//                    if (dp[i][k] + dp[k][j] < dp[i][j]) {
//                        dp[i][j] = NEGATIVE_INFINITY;
//                        next[i][j] = REACHES_NEGATIVE_CYCLE;
//                    }

        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                int[] r1 = fromInd(i);
                int[] r2 = fromInd(next[i][j]);
                if(r2[0]>r1[0]){
                    //System.out.print("D");
                    next_dir[i][j] = Direction.DOWN;
                }else{
                    if(r2[0]<r1[0]){
                        // System.out.print("U");
                        next_dir[i][j] = Direction.UP;
                    }else{
                        // System.out.println(i+"**"+j);
                        // System.out.println(next_dir[i][j]);
                        //System.out.print("X");
                        next_dir[i][j] =   r2[1]>r1[1] ? Direction.RIGHT : Direction.LEFT;
                    }
                }
            }
        }

        solved = true;
    }



    /* Example usage. */

    // Creates a graph with n nodes. The adjacency matrix is constructed
    // such that the value of going from a node to itself is 0.
    public static double[][] createGraph(int n) {
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            java.util.Arrays.fill(matrix[i], POSITIVE_INFINITY);
            matrix[i][i] = 0;
        }
        return matrix;
    }



    public int toInd(int row, int col){
        return row*n_col+col;
    }

    public int toInd(Cell c){
        return c.getRow()*n_col+c.getColumn();
    }

    public static int[] fromInd(int ind){
        return new int[]{ind/n_col,ind%n_col};
    }

    public double setDist(int[] set1,int[] set2){
        double[] distances = new double[set1.length*set2.length];
        for(int i1 = 0 ; i1<set1.length; i1++){
            for(int i2 = 0; i2<set2.length; i2++){
                if(set1[i1]==-1 || set2[i2]==-1){
                    distances[i2*set1.length + i1] = POSITIVE_INFINITY;
                }else{
                    distances[i2*set1.length + i1] = dp[set1[i1]][set2[i2]];
                }

            }
        }
        return Arrays.stream(distances).min().getAsDouble();
    }

    public int getIndClosestInSet(int src,int[] dsts){
        // dsts SHOULD NOT BE -1
        double[] distances = new double[dsts.length];

        for(int i = 0; i<dsts.length; i++){
            distances[i] = dp[src][dsts[i]];
        }
        double min = 1000000.0;
        int indx = 0;
        for(int j = 0; j<distances.length;j++){
            double newdist = distances[j];
            if(newdist<min){
                min = newdist;
                indx = j;
            }
        }

        return dsts[indx];
    }

    public int getDistClosestInSet(int src,int[] dsts){
        // dsts SHOULD NOT BE -1
        double[] distances = new double[dsts.length];

        for(int i = 0; i<dsts.length; i++){
            distances[i] = dp[src][dsts[i]];
        }
        double min = 1000000.0;
        int indx = 0;
        for(int j = 0; j<distances.length;j++){
            double newdist = distances[j];
            if(newdist<min){
                min = newdist;
                indx = j;
            }
        }

        return (int)min;
    }

    public int getIndClosestInSet(int src,List<Integer> dstsa){
        Integer[] dsts = dstsa.stream().toArray(Integer[]::new);
        // dsts SHOULD NOT BE -1
        double[] distances = new double[dsts.length];

        for(int i = 0; i<dsts.length; i++){
            distances[i] = dp[src][dsts[i]];
        }
        double min = 1000000.0;
        int indx = 0;
        for(int j = 0; j<distances.length;j++){
            double newdist = distances[j];
            if(newdist<min){
                min = newdist;
                indx = j;
            }
        }

        return dsts[indx];
    }

    public Direction intToDir(int i){
        switch (i%4){
            case 0: return Direction.UP;
            case 1: return Direction.LEFT;
            case 2: return Direction.DOWN;
            case 3: return Direction.RIGHT;
            default: return Direction.UP;
        }
    }

    public int dirToInt(Direction dir){
        switch (dir){
            case UP: return 0;
            case LEFT: return 1;
            case DOWN: return 2;
            case RIGHT: return 3;
            default: return 0;
        }
    }

    public int nxtInd(int cur_ind,Direction dir){
        int i = cur_ind/n_col;
        int j = cur_ind%n_col;
        switch(dir){
            case DOWN: {
                if(i+1>=n_row) return -1;
                return cur_ind+n_col;
            }
            case UP: {
                if(i-1<0) return -1;
                return  cur_ind-n_col;
            }
            case LEFT: {
                if(j-1<0) return -1;
                return cur_ind-1;
            }
            case RIGHT: {
                if(j+1>=n_col) return  -1;
                return  cur_ind+1;
            }
            // DEFAULT SHOULD THROW EXCEPTION
            default: return -1;
        }
    }

    // CAUTION: NOT FAULT-TOLERANT NO SILLY QUERIES!
    public Cell nxtCell(Cell c,Direction dir,Map map){
        int i2 = c.getRow();
        int j2 = c.getColumn();
        switch(dir){
            case DOWN: {
                i2 = i2 + 1; break;
            }
            case UP: {
                i2= i2 - 1; break;
            }
            case LEFT: {
                j2= j2 - 1; break;
            }
            case RIGHT: {
                j2= j2 + 1; break;
            }
            // DEFAULT SHOULD THROW EXCEPTION
            default: break;
        }
        return map.getCell(i2,j2);
    }

//    public int minSetDist(int[] set1,int[] set2){
//
//    }

}