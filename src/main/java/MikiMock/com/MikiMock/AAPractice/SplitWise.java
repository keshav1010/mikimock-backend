package MikiMock.com.MikiMock.AAPractice;

import lombok.RequiredArgsConstructor;

class Member{

    private String name;
    private Double cost = 0.00;

     public Member(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setCost(Double cost){
         this.cost += cost;
    }

    public Double getCost(){
        return cost;
    }

    public Double extraSpend(Double avgCost){
        if(avgCost >= cost) return 0.00;
        else return cost - avgCost;
    }

}




@RequiredArgsConstructor
public class SplitWise {

    private final Member member;
    private final Member member2;
    private final Member member3;
    private String tripName;

    private Double totalCost = 0.00;
    private Double avgCost = 0.00;

    public SplitWise(Member member, Member member2, Member member3, String tripName ){
        this.member = member;
        this.member3 = member3;
        this.tripName = tripName;
        this.member2 = member2;
    }

    public void addCost(Member member , Double cost){
        this.totalCost += cost;
        avgCost = totalCost/3;
        member.setCost(cost);
    }

    public void getInvoice(){
        System.out.println("Total cost - " + totalCost );
        System.out.println("Avg Cost- " + avgCost );
    }

    public void getMyDetails(Member member){
        System.out.println("Total cost - " + member.getCost() );
        System.out.println("Avg Cost- " + member.extraSpend(avgCost) );
    }

}


class Main{


    public static void Main(String args[]){
        Member m1 = new Member("Keshav");
        Member m2 = new Member("Kartikey");
        Member m3 = new Member("Aditya");

        SplitWise splitWise = new SplitWise(m1,m2,m3,"Rajasthan");

        splitWise.addCost(m1,8932.00);
        splitWise.addCost(m2,15732.00);
        splitWise.addCost(m3,8932.00);
        splitWise.addCost(m1,892.00);
        splitWise.addCost(m2,9932.00);

        splitWise.getInvoice();
        splitWise.getMyDetails(m1);
        splitWise.getMyDetails(m2);
        splitWise.getMyDetails(m3);
    }
}
