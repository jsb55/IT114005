public class HW2
{
   public static void main(String[] args) 
   {
       int num[] = {1,2,3,4,5,6,7,8,9,10,11,12};

      for (int i = 0; i < num.length; i++)
        System.out.println(num[i]);

      for (int i = 0; i < num.length; i++)
         if (num[i] % 2 == 0) 
            System.out.print(num[i]+ " ");


   }
}


/* I first created the array "num[]" and assigned numbers 1-12.
   After this, I created a for loop that would add '1' to 'i'
   until 'i' was greater than the length of the array (12). Each
   time the loop ran, it would print each value of the array.

   The next for loop does the same thing, but prints the numbers only
   if they are divisible by 2.
*/
