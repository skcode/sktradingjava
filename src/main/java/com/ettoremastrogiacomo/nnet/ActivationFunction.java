/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.nnet;

/**
 *
 * @author a241448
 */
public class ActivationFunction {

    public enum FUNCTIONS {
        SIGMOID,TANH,IDENTITY,RELU,RELUP
    };
    public final java.util.function.Function<Double, Double> eval, derivative;

    ActivationFunction(FUNCTIONS fun) {
        switch (fun) {
            case SIGMOID:
                eval = ActivationFunctions::sigmoid;
                derivative = ActivationFunctions::sigmoidDerivative;
                break;
            case TANH:        
                eval = ActivationFunctions::tanh;
                derivative = ActivationFunctions::tanhDerivative;
                break;
            case IDENTITY:
                eval = ActivationFunctions::identity;
                derivative = ActivationFunctions::identityDerivative;
                break;                
            case  RELU:
                eval = ActivationFunctions::reLU;
                derivative = ActivationFunctions::reLUDerivative;
                break;                
            case  RELUP:
                eval = ActivationFunctions::reLUp;
                derivative = ActivationFunctions::reLUpDerivative;
                break;                                
            default:
                throw new IllegalArgumentException(fun + " not implemented");
        }
    }

}

class ActivationFunctions {

    public static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    public static double sigmoidDerivative(double x) {
        double t = sigmoid(x);
        return t * (1 - t);
    }

    public static double tanh(double x) {
        return (Math.exp(x)-Math.exp(-x))/(Math.exp(x)+Math.exp(-x));
    }

    public static double tanhDerivative(double x) {
        double t = tanh(x);
        return 1-t*t;
    }
    public static double identity(double x) {return x;}
    public static double identityDerivative(double x) {return 1;}
    public static double reLU(double x) {
        return x<0?0:x;
    }
    public static double reLUDerivative(double x) {
            return x<0?0:1;
    }
    public static double reLUp(double x) {
        return x<0?0.01*x:x;
    }
    public static double reLUpDerivative(double x) {
            return x<0?0.01:1;
    }
}
