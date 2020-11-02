import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logs{

    public static  void log_tcp_connection_to(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id1 + "] makes a connection to Peer [" + id2 + "].");
        try {
            logWriter.write(log_entry.toString());
            logWriter.newLine();
            logWriter.flush();
        }catch(Exception e){

        }
    }

    public static  void log_tcp_connection_from(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id1 + "] is connected from Peer [" + id2 + "].");
        try{
            logWriter.write(log_entry.toString());
            logWriter.newLine();
            logWriter.flush();
        }catch(Exception e) {
        }


    }

    //following function is done
    public static void log_change_of_preferred_neighbors(BufferedWriter logWriter, int id, int[] id_list){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp +": Peer [" + id + "] has the preferred neighbors [" );

        String result_string = "";
        StringBuilder sb = new StringBuilder();
        String s;
        for (int i : id_list) {
            s = Integer.toString(i);
            sb.append(s).append(",");
        }
        result_string = sb.deleteCharAt(sb.length() - 1).toString();

        log_entry.append(result_string);

        log_entry.append("].");

        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    public static void log_change_of_optimistically_unchoked_neighbor(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + "] has the optimistically unchoked neighbor [" + id2 + "].");


        try{
            logWriter.write(log_entry.toString());

        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    public static void log_unchoking(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id1 + "] is unchoked by [" + id2 + "].");
        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    public static void log_choking(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id1  +"] is choked by ["+ id2 +"].");
        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    //following function is done
    public static void log_receiving_have_message(BufferedWriter logWriter, int id1, int id2, int index){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        StringBuffer log_entry = new StringBuffer();
        log_entry.append(timeStamp + ": Peer [" + id1 +"] received 'have' message from [" + id2+ "] for the piece: " + index + ".");

        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }
    }

    public static void log_receiving_interested_message(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id1 + "] received the 'interested' message from [" + id2 + "]." );
        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    public static void log_receiving_not_interested_message(BufferedWriter logWriter, int id1, int id2){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" +id1 + "] received the 'not interested' message from [" + id2 + "]." );

        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    //following function is done
    public static void log_downloading_a_piece(BufferedWriter logWriter, int id1, int id2, int index, int number_of_pieces){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id1 + "] has downloaded the piece " + index + " from [" + id2 + "]. " +"Now the number of pieces it has is : "+ number_of_pieces + ".");

        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }

    }

    //following function is done
    public static void log_completion_of_download(BufferedWriter logWriter, int id){

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

        StringBuffer log_entry = new StringBuffer();

        log_entry.append(timeStamp + ": Peer [" + id + "] has downloaded the complete file.");

        try{
        logWriter.write(log_entry.toString());
        logWriter.newLine();
        logWriter.flush();
        }catch(Exception e) {
        }
    }

}