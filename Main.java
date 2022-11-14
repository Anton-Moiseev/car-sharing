package carsharing;

public class Main {

    public static void main(String[] args) {
        CarSharingProgram carSharingProgram = new CarSharingProgram();
        try {
            carSharingProgram.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}