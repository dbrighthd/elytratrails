package dbrighthd.elytratrails.controller;

import dbrighthd.elytratrails.config.ModConfig;

public class EasingUtil {
    public static double easeBothSin(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return  0.5 - 0.5 * Math.cos(Math.PI * t);
    }
    public static double easeOutSin(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return Math.sin((Math.PI * 0.5) * t);
    }
    public static double easeInSin(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return (1.0 - Math.cos((Math.PI * 0.5) * t));
    }
    public static double easeBothBack(double t)
    {
        double s = 1.70158;
        double s1 = 2.5949095;
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        if(t <= 0.5)
        {
            return 2 * t * t * (2*t*(s1 + 1) - s1);
        }
        return 0.5* (Math.pow(((2*t) - 2),2)) * ((2*t-2) * (s1 + 1) + s1) +1;
    }
    public static double easeOutBack(double t)
    {
        double s = 2.5949095;
        double s1 = 2.5949095;
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return Math.pow((t-1),2) * ((t-1) * (s + 1) + s) + 1;
    }
    public static double easeInBack(double t)
    {
        double s = 2.5949095;
        double s1 = 2.5949095;
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1) {
            return 1;
        }
        return t*t*(t*(s+1)-s);
    }
    public static double noEase(double t)
    {
        return t;
    }
    public static double easeIn(double t, EaseType easeType)
    {
        if(easeType == EaseType.Back)
        {
            return easeInBack(t);
        }
        if(easeType == EaseType.None)
        {
            return noEase(t);
        }
        return easeInSin(t);
    }
    public static double easeOut(double t, EaseType easeType)
    {
        if(easeType == EaseType.Back)
        {
            return easeOutBack(t);
        }
        if(easeType == EaseType.None)
        {
            return noEase(t);
        }
        return easeOutSin(t);
    }
    public static double easeBoth(double t, EaseType easeType)
    {
        if(easeType == EaseType.Back)
        {
            return easeBothBack(t);
        }
        if(easeType == EaseType.None)
        {
            return noEase(t);
        }
        return easeBothSin(t);
    }

    public enum EaseType {
        Sine,
        Back,
        None,
    }
}
