package in.skyrideraj.testpaypalintegration.utilities;

public class StringUtilities{

    //check if string is decimal
    public static boolean isNumeric(String s){
        if(s.isEmpty())
            return false;
        if(s.matches("\\d+(?:\\.\\d+)?"))
            return true;
        return false;
    }
}
