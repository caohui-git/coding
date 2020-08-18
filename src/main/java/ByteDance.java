import java.util.Arrays;

public class ByteDance {

    public static  void main(String[] args){
//        int[] num = {1,-2,3,-4,5};
//        int n=num.length;
//        int[] s = Arrays.copyOf(num,n);
//        int res = Integer.MIN_VALUE;
//        for(int nu:num)
//             res= Math.max(nu,res);
//        for(int l=2;l<=n;++l){
//            for(int i =0;i<=n-l;++i){
//                if(l%2==1){
//                    s[i]=s[i]+num[i+l-1];
//                }else{
//                    s[i]=s[i]-num[i+l-1];
//                }
//                System.out.println(s[i]);
//                res=Math.max(s[i],res);
//            }
//        }
//        System.out.println(res);
        int n =4;
        int[] a={3,3,3,3};
        int[] b={1,1,1,1};
        int p =100;
        Arrays.sort(a);
        Arrays.sort(b);
        int res=1;
        for(int bi=n-1;bi>0;bi--){
            int ai=bi;
            while(ai>=0&&a[ai]>=b[bi]){
                ai--;
            }
            res=res*(bi-ai);
        }
        System.out.println(res%p);
    }
}
