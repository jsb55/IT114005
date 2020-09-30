public class RecursionToLoop {

	public static int sum(int num) {

		int num2 = 0;

		for (int i = 1; i < num; ++i) {

			num2 = num2 + i;
		}

		return num2 + num;
	}

	public static void main(String[] args) {

		System.out.println(sum(10));
	}
}