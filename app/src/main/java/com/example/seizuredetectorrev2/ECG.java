package com.example.seizuredetectorrev2;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ECG {
    private double [] signal;
    private int window; // this window specifies how many samples I sould use to display.
    private double[] time;
    private int fs;
    private int peakCounter;
    private double[] positiveECG;
    // thresholds
    private double positiveT;
    private double positiveT2;
    private double negativeT;
    private double negativeT2;
    //------ DEBUG -------------------------------
    private static final String TAG = "ECG class";
    //-------------------------------------------
    // setting up the window to select the time i want to view
    //.... and the constructor
    public ECG (int fs, int window, double[] signal){
        this.window = window;
        this.fs = fs;
        this.signal = signal;
    }

    public double[] getTime(){
        // make an array of time
        double[] time = new double[window];
        for (int i=1 ;  i < time.length -3 ; i++ ){
            int temp = i-1;
            time[temp]= i/fs;
            Log.d(TAG , "each interaction" + time[temp]);
        }
        //-------- Debug
        Log.d(TAG , "Fs is : " + fs +"\n" +"the time array is"  + time [0] +"...." + time [1] +"...." +Arrays.toString(time));
        //-------------



        return time;
    }


    public void getQRS (){
        // Initialize T+
        int fiveSecs = this.fs*5; // is 1000 samples
        //------ DEBUG -------------------------------
        if (fiveSecs> signal.length)
            Log.d(TAG, "length of signal > 5 seconds");
        //-------------------------------------------


        double[] fiveSecArray= new double[fiveSecs]; //new array where I calculate the third largest peak
        // filling the array.....
        for (int i = 0 ; i < fiveSecArray.length -1 ; i++){
            fiveSecArray[i] = signal[i];
        }

        Arrays.sort(fiveSecArray); // sorting the array

        // finding T positive
        positiveT = fiveSecArray[fiveSecArray.length-3]; // the third peak is the T_positive
        Log.d(TAG, "the third max value in first 5 sec is "+ positiveT);
        positiveT /= 1.5; // adapting the T_positive

        // T positive 2
        positiveT2 = positiveT * 0.5;

        // finding T negative
        negativeT = fiveSecArray[3]; // the third dip is the T_negative
        Log.d(TAG, "the third minimum value in first 5 sec is "+ negativeT);
        negativeT /= 1.5; // adapting the T_negative


        // T negative 2
        negativeT2 = negativeT * 0.5;

        //----------END of thresold calculation debug
        Log.d(TAG, " T positive "+ positiveT + " T positive 2 " + positiveT2 + "negative T " + negativeT + " negative T 2 " + negativeT2);
        // --------

        // create a vector with only the positive part
        // empty vector....
        double [] positiveECG = new double[signal.length];

        // fill the vector with zeros
        Arrays.fill(positiveECG, 0);
        // fill the empty vector
        for (int i=0 ; i<signal.length ; i++){
            if (signal[i] > 0){
                positiveECG[i] = signal[i];
            }
        }
        this.positiveECG = positiveECG;
        // positiveECG is an array of double containing the raw positive signal


//        return positiveECG;
    }

    public void detectQRS(){
        int signalLength = this.signal.length;
        int i;
        double positiveP;
        int peakCounter = 0; // court R peaks
        double [] peakVector = new double[signalLength]; // array of only positive peaks
        // fill with zeros
        for (i = 0; i< signalLength; i++){
            peakVector[i] = 0;
        }

        for (i = 0; i<signalLength ; i++){
            if (this.positiveECG [i] > positiveT){
                peakVector[i] = positiveECG[i];
                peakCounter ++; // increment the counter
            }
            else if (this.positiveECG [i] < positiveT && this.positiveECG [i] > positiveT){
                // update T and search again
                //T_positive = 0.8 * T_positive + 0.2 * P_positive;
                positiveP = this.positiveECG[i];
                this.positiveT = 0.8 * this.positiveT + 0.2 * this.positiveECG[i];
                // and search again
                if (this.positiveECG[i] > positiveT){
                    peakVector[i] = positiveECG[i];
                    peakCounter ++; // increment the counter
                }
            }
        }

        // adjust the vector.... and keep only one peak

        for (i=0;i<signalLength; i++){
            //put at zero 8 values above that peak
            if (peakVector[i] > 0 ){
                for(int j = 1; j < 9 ; j++){
                    peakVector[i+j] = 0;
                }
            }
        }

        // return....
        this.positiveECG = peakVector;
        this.peakCounter = peakCounter;
    }
    //TODO add get HR method
    public double getHRnumber(int index){
        double hr = 0;
        int difference;
        //this method calculates only the difference between two RR peaks in time, so I can show it real time
        int indexWindow = 500; // go back 500 samples and find the first positive peak
        if (positiveECG[index]>0 && index > indexWindow){
            // Search for the last positive value and calculate HR using the index
            for (int j = 1; j <= indexWindow ; j++){
                if (positiveECG[index-j] > 0){
                    //ok I found it.....
                    //calculate how many samples....
                    difference = index- indexWindow -j;
                    hr = 60/ (difference/fs);
                    //----------DEBUG
                    Log.d(TAG, "difference = " + difference);
                    // --------
                    break;
                }
            }
        }
        return hr;
    }
    //TODO add get HR method THAT RETURNS ARRAY OF HR




    public void detectQRSSearchback(){
        // begin QRS detection
        int i;
        int peakCounter = 0;
        int [] peakLocation = new int[signal.length]; // temporany vector of only the location of the peaks in time
        // fill peak location with zeros
        for (i = 0 ; i< peakLocation.length; i++){
            peakLocation[i] = 0;
        }
        double [] peakBuffer = new double[signal.length]; // amplitudes of QRS
        // fill peak buffer with zeros        Arrays.fill(peakBuffer, 0);
        for (i = 0 ; i< peakBuffer.length; i++){
            peakBuffer[i] = 0;
        }

        double mean8peaks = 0; //in time (not in samples)

//        for ( int i = 0 ; i < signal.length ; i++){
//            if (positiveECG [i] >= positiveT) {
//                peakBuffer [i] = positiveECG[i];
//                peakCounter ++;
//                peakLocation[peakCounter] = i;
//                // store the mean of 8 peaks
//                if (peakLocation.length >= 8){
//                    double[] tempVec = new double[8];
//                    for (i= 0; i<8 ; i++){
//                        tempVec[tempVec.length -1 -i] = peakLocation [peakLocation.length - 1 - i];
//                    }
//                    double sum = 0;
//                    // calculating the mean
//
//                    for (i = 0 ; i<tempVec.length; i++){
//                        sum += tempVec[i];
//                    }
//                    mean8peaks = sum / tempVec.length;
//                    //----------DEBUG
//                    Log.d(TAG, "length of 8 peaks vector = " + tempVec.length + "\n mean of 8 peaks = " + mean8peaks);
//                    // --------
//
//                }
//            }
//        }


    }

    public double[] getRawSignal(){
        return this.signal;
    }

    public double[] getSignal(){
        return this.positiveECG;
    }

    public int retunPeakCounter(){
        return this.peakCounter;
    }
}
