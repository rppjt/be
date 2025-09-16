package runrush.be.runningrecord.domain;

public interface LocationResolver {
    String resolveLocationName(double latitude, double longitude);
}