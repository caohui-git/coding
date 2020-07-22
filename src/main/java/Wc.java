
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;

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
    //一轮partition
    public int partition(int[] nums, int low ,int high){
        if(low < high){
            int pivot = nums[low];
            while(low < high){
                while (low < high && nums[high] >= pivot) high--;
                nums[low] = nums[high];
                while (low < high && nums[low] <= pivot) low++;
                nums[high] = nums[low];
            }
            nums[low] = pivot;
        }
        return low;
    }
    //最小的前k个数
    public int[] Kst_small(int[] nums, int k){
        int start = 0, end = nums.length - 1;
        int index = partition(nums, start, end);
        while(index != k-1){
            if(index < k-1){
                start = index +1;
                index = partition(nums, start, end);
            }else{
                end = index -1;
                index = partition(nums, start, end);
            }
        }
        int[] res = copyOfRange(nums, 0, k);
        return res;
    }
    //堆排序下溢算法
    public void adjustdown(int[] A, int pos){
        int tmp = A[pos];
        for(int i = pos * 2 + 1; i < A.length; i = i * 2 +1){
            if(i < A.length - 1 && A[i] < A[i + 1])
                i++;
            //和向下移动的节点相比
            if(A[i] < tmp)break;
            else{
                A[pos] = A[i];
                pos = i;
            }
        }
        A[pos] = tmp;
    }
    //建堆
    public void buildheap(int[] h){
        for(int i = h.length / 2 -1; i >= 0; i--)
            adjustdown(h, i);
        System.out.println(Arrays.toString(h));
    }
    //取最小的前k个数
    public int[] Kst_small_with_bigrootheap(int[] nums, int k){
        int[] res = copyOfRange(nums, 0, k);
        buildheap(res);
        for(int i = k; i < nums.length; ++i){
            if(nums[i] < res[0]){
                res[0] = nums[i];
                adjustdown(res, 0);
            }
        }
        return res;
    }
    public static void main(String[] args){
        int[] nums = {1, 2, 7, 4, 2, 9, 12, 1};
        Wc wc = new Wc();
        //wc.qs(nums, 0 ,nums.length - 1);
        System.out.println(Arrays.toString(wc.Kst_small_with_bigrootheap(nums, 4)));
    }
}
