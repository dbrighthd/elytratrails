package dbrighthd.elytratrails.util;

import java.util.Random;

/**
 * sets up easings for twirling. I got the equations from <a href="https://www.desmos.com/calculator/m8myals511">this desmos tool</a>
 */
public class EasingUtil {
    static Random random = new Random();
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
    public static double easeRandom(double t)
    {
        return random.nextDouble();
    }
    public static double easeInCubic(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return t*t*t;
    }
    public static double easeOutCubic(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return Math.pow(t-1,3)+1;
    }
    public static double easeBothCubic(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        if( t <= 0.5)
        {
            return 4*Math.pow(t,3);
        }
        return 4*Math.pow(t-1,3)+1;
    }
    public static double easeBothElastic(double t)
    {
        double p = 0.3;
        double p1 = 0.45;
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        if(t <= 0.5)
        {
            return -0.5*Math.pow(2,(20*t-10))*Math.sin((2*Math.PI*(2*t-1.1125)/p1));
        }
        return 0.5*Math.pow(2,(-20*t+10))*Math.sin((2*Math.PI*(2*t-1.1125)/p1));
    }
    public static double easeInElastic(double t)
    {
        double p = 0.45;
        double p1 = 0.45;
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return -Math.pow(2,10*(t-1))*Math.sin((2*Math.PI*(t-1-(p/4)))/p);
    }
    public static double easeOutElastic(double t)
    {
        double p = 0.45;
        double p1 = 0.45;
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1){
            return 1;
        }
        return Math.pow(2,-10*t)*Math.sin((2*Math.PI*(t-(p/4)))/p)+1;
    }
    public static double easeBothBack(double t)
    {
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
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1) {
            return 1;
        }
        return t*t*(t*(s+1)-s);
    }
    public static double easeInExpo(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1) {
            return 1;
        }
        return Math.pow(2,10*t-10)-0.001;
    }
    public static double easeOutExpo(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1) {
            return 1;
        }
        return 1.001*-Math.pow(2,-10*t)+1;
    }
    public static double easeBothExpo(double t)
    {
        if(t < 0)
        {
            return 0;
        }
        if( t >= 1) {
            return 1;
        }
        if (t <= 0.5)
        {
            return 0.5*Math.pow(2,20*t-10)-0.0005;
        }
        return 0.50025*-Math.pow(2,-20*t+10)+1;
    }
    public static double noEase(double t)
    {
        return t;
    }
    public static double easeIn(double t, EaseType easeType)
    {
        switch(easeType)
        {
            case Back -> {return easeInBack(t);}
            case None -> {return noEase(t);}
            case Cubic -> {return easeInCubic(t);}
            case Expo -> {return easeInExpo(t);}
            case Elastic -> {return easeInElastic(t);}
            case Random -> {return easeRandom(t);}
            default -> {return easeInSin(t);}
        }
    }
    public static double easeOut(double t, EaseType easeType)
    {
        switch(easeType)
        {
            case Back -> {return easeOutBack(t);}
            case None -> {return noEase(t);}
            case Cubic -> {return easeOutCubic(t);}
            case Expo -> {return easeOutExpo(t);}
            case Elastic -> {return easeOutElastic(t);}
            case Random -> {return easeRandom(t);}
            default -> {return easeOutSin(t);}
        }
    }
    public static double easeBoth(double t, EaseType easeType)
    {
        switch(easeType)
        {
            case Back -> {return easeBothBack(t);}
            case None -> {return noEase(t);}
            case Cubic -> {return easeBothCubic(t);}
            case Expo -> {return easeBothExpo(t);}
            case Elastic -> {return easeBothElastic(t);}
            case Random -> {return easeRandom(t);}
            default -> {return easeBothSin(t);}
        }
    }

    public enum EaseType {
        Back,
        @SuppressWarnings("unused") Sine, // it thinks its unused because it's not in the code (it's a fallback) but it can be selected by the player
        Cubic,
        Expo,
        Elastic,
        Random,
        None,
    }

    /**
     * value to multiply by to match the derivative of the continued twirl
     */
    public static double getEaseMult(EaseType easeType)
    {
        switch(easeType)
        {
            case Back -> {return 2.9931;}
            case Cubic -> {return 1.9099;}
            case None -> {return 0.6366;}
            case Expo -> {return 4.4171;}
            case Elastic -> {return 4.4127;}
            default -> {return 1;}
        }
    }
}
