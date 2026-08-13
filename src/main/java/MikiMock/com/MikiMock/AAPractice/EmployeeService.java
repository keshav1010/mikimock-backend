package MikiMock.com.MikiMock.AAPractice;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;

public interface EmployeeService {

    void process();
}


@Primary
class EmployeeServiceImp1 implements EmployeeService{

    public void process(){
        System.out.println("processing 1");
    }
}

class EmployeeServiceImp2 implements EmployeeService{

    public void process(){
        System.out.println("processing 2");
    }
}

class Manager{

    private final EmployeeService employeeService;

//    Option - 1           Implementation class already annotated by primary
//    Manager(@Qualifier(EmployeeService employeeService) {
//        this.employeeService = employeeService;
//    }


//    Option - 2          Use Qualifier
    Manager(@Qualifier("EmployeeServiceImp2") EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

}



