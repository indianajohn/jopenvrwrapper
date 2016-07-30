package vrprovider;

public class HelloWorld {
    public static void main(String[] args) {
        VRProvider provider = new VRProvider();
        try {
            provider.vrState.addControllerListener(new SampleControllerListener());
            Thread vrPoller = new Thread(provider, "vrPoller");
            vrPoller.start();
            vrPoller.join();
        } catch (Exception e) {
            System.out.println("Unhandled exception: " + e.toString());
        }
        System.out.println("Exited normally.");
    }
}