package ga.rugal;

public class DebuggeeMain {

  public static void main(String[] args) throws InterruptedException {
    int count = 0;
    while (count < 10) {
      System.out.println(count);
      Thread.sleep(1000);
      count += 1;
    }
  }
}
