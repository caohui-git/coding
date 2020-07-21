import java.util.ArrayList;
import java.util.Arrays;

public class Wc {
    public Wc(){
        System.out.println("ch");
    }
    //快速排序
    public void qs(int[] nums, int start, int end){
        int low = start, high = end;
        if(low < high) {
            int pivot = nums[low];
            while (low < high) {
                while (low < high && nums[high] >= pivot) high--;
                nums[low] = nums[high];
                while (low < high && nums[low] <= pivot) low++;
                nums[high] = nums[low];
            }
            nums[low] = pivot;
            System.out.println(Arrays.toString(nums));
            qs(nums, start, low - 1);
            qs(nums, low + 1, end);
        }
    }
    public static void main(String[] args){
        int[] nums = {1, 2, 7, 4, 2, 9, 12, 1};
        Wc wc = new Wc();
        wc.qs(nums, 0 ,nums.length - 1);
    }
}
