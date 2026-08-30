public class TestLoad {
    public static void main(String[] a) {
        try {
            System.load(a[0]);
            System.out.println("LOAD OK");
        } catch (Throwable t) {
            System.out.println("FAIL: " + t.getMessage());
        }
    }
}
