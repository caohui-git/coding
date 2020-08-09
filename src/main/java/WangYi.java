public class WangYi {
    //找出和一个整数的组成相同的，比该整数小的最大的数
    public static  long smallThanNum(int num){
        char[] ns = String.valueOf(num).toCharArray();
        int n = ns.length;
        int tmp = 0;
        for(int i=n-1;i > 0; --i ){
            if(ns[i] > ns[i-1])continue;
            else{
                tmp =i;
                break;
            }
        }
        if(tmp == 0)return 0;
        int low =tmp,high=n-1;
        int index = 0;
        while(low<high){
            int mid = low +(high-low)/2;
            if(ns[mid] == ns[tmp - 1]){
                index =mid;
                break;
            }
            else if(ns[mid]<ns[tmp - 1])
                low =mid +1;
            else
                high = mid -1;
        }
        if(index ==0)
            index = low;
        if(ns[0] == '0')
            return 0;
        char c = ns[tmp - 1];
        ns[tmp - 1] = ns[index];
        ns[index] = c;
        String nstr= new String(ns);
        StringBuilder sb = new StringBuilder();
        sb.append(nstr.substring(tmp));
        return Long.parseLong(nstr.substring(0,tmp)+(sb.reverse().toString()));
    }

    public static void main(String[] args){
        System.out.println(smallThanNum(79237492));
    }
}
