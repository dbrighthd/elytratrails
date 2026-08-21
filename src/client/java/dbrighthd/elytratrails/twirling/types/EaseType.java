package dbrighthd.elytratrails.twirling.types;

public interface EaseType {
    public String id();

    default double easeIn(double t){
        return t;
    }

    default double easeOut(double t){
        return t;
    }

    default double easeInOut(double t){
        return t;
    }

    default double easeMult(){return 0.6366;}

    default double flipTime(){return 2;}

    default double flipStart(){return 0;}
}
