import java.io.File; 
import java.util.Scanner;
import java.io.PrintWriter;

/* [AllLevels.java]
 * Version: 8.0
 * Author: Candice Zhang
 * Date: November 4, 2019
 * Description: Determines the best route for duber for different levels to:
 *              (level 4 and below)  minimize steps
 *              (level 4+ and above) maximize tips, with access to microwaves
 * Note: *In this program, Duber is allowed to pass through a microwave without using it
 */

class AllLevels {
    public static char[][] blankMap;
    public static char[][] map;
    
    public static int[] startPos;
    public static int[][] deliveries;
    public static int[][] visitables;
    
    public static char[][] path;
    public static char[][] solution;
    public static int[][] bestOrder;
    public static int bestSteps = Integer.MAX_VALUE;
    public static int bestTips = Integer.MIN_VALUE;
    
    public static void main(String[] args)throws Exception{
        
        Scanner input = new Scanner(System.in);
        System.out.println("input file name (ends with '.txt'):");
        String fileName = input.nextLine();
        String ppmFileName = fileName.substring(0,fileName.length()-4)+".ppm";
            
        System.out.println("Duber, do you care about tips?\nEnter 'yes' for level 4+ and above, enter anything else for level 4 and below");
        String levelChoice = input.nextLine();
        boolean considerTips;
        if(levelChoice.equals("yes")){
            considerTips = true;
        }else{
            considerTips = false;
        }
        input.close();
        
        // initiation
        solution = readMap(fileName);
        blankMap = readMap(fileName);
        map = readMap(fileName);
        path = readMap(fileName);
        
        startPos = findStart(map);
        deliveries = findDeliveries(map);
        visitables = findVisitables(map);
        int[][] order = new int[visitables.length][2];
        
        for(int i = 0; i<order.length; i++) {
            order[i] = new int[]{-1,-1};
        }
        
        // find out the best order among different lengths (different numbers of microwaves used, if any)
        for(int len=deliveries.length; len<=visitables.length; len++){
            findBestOrder(visitables, order, len, considerTips);
        }
        
        // record the path of best solution to a 2d char array
        for(int i=0;i<order.length;i++){
            if(i==0){
                findShortestPath(map, startPos[0], startPos[1], bestOrder[0][0], bestOrder[0][1], Integer.MAX_VALUE, 0,true);
            }else{
                findShortestPath(map, bestOrder[i-1][0], bestOrder[i-1][1], bestOrder[i][0], bestOrder[i][1], Integer.MAX_VALUE, 0,true);
            }
            solution = recordPath(path, solution);
        }
        
        // output results
        if(considerTips==true){
            System.out.print("Tips: ");
            System.out.println(bestTips);
        }
        
        System.out.print("Steps: ");
        System.out.println(bestSteps);
        
        System.out.println("Path: ");
        print2dCharArr(solution);
        
        System.out.println("Outputting ppm file...");
        outputPPM(ppmFileName,solution,500);
        System.out.println("Output completed\nOutputted to: "+ppmFileName);
        
    }
    
    /**
     * findBestOrder 
     * Generate all possible combinations of visitable locations and finds the best order
     * @param visitables, a 2D int array that stores [x,y] of all visitable locations(deliveries and microwaves)
     * @param order, a 2D int array that stores the combination generated
     * @param remain, an integer that keeps track of the remaining blank spot in the order array
     * @param considerTips, a boolean variable to helps determine whether tips should be considered
     * @return nothing
     */
    public static void findBestOrder(int[][] visitables, int[][] order, int remain, boolean considerTips){ 
        if(remain==0){  // a combination is generated
            if(considerTips==false){
                updateBestOrderLevel3(order);
            }else{
                // check if all deliveries have been reached in this combination
                int count = 0;
                for(int i=0;i<order.length;i++){
                    int x = order[i][0], y = order[i][1];
                    if(x==-1 && y==-1){
                        continue;
                    }
                    if(isDelivery(blankMap[y][x])){
                        count++;
                    }
                }
                if(count==deliveries.length){
                    // compare to the current best solution
                    updateBestOrderLevel4PP(order);
                }
            }
        }else{
            for(int i = 0; i<visitables.length; i++) {
                int[] pos = visitables[i];
                if(!isInOrder(pos,order)){
                    findBestOrder(visitables,addToOrder(visitables[i],order),remain-1,considerTips);
                }
            }
        }
    }    

    /**
     * updateBestOrderLevel4PP 
     * Determines whether the order should be updated as the best order, in terms of tips
     * If there is a tie in tips, the order with fewer total steps is the better one
     * @param order, a 2D int array that is used to compare with the current best order
     * @return nothing
     */
    public static void updateBestOrderLevel4PP(int[][] order){
        int[] result = tipsAndStepsOf(order);
        int tips = result[0];
        int steps = result[1];
        if( (tips>bestTips) || (tips==bestTips && steps<bestSteps) ){
            bestTips = tips;
            bestSteps = steps;
            bestOrder = new int[order.length][2];
            for(int i = 0; i<order.length; i++){
                bestOrder[i][0] = order[i][0];
                bestOrder[i][1] = order[i][1];
            }
        }
    }
    
    /**
     * tipsAndStepsOf 
     * Determines the total tips and steps of a given order of locations
     * @param order, a 2D int array that is used to calculate tips and steps
     * @return result, an int array that contains the tips and the steps [int tips, int steps]
     */
    public static int[] tipsAndStepsOf(int[][] order){
        int[] result = new int[2];
        int totalSteps = 0;  
        int tipSteps = 0;
        int curTips = 0;
        int totalTips = 0;
        
        for(int i=0;i<order.length;i++){
            int startX, startY;
            int destX, destY;
            destX  = order[i][0];
            destY  = order[i][1];
            if(destX==-1 && destY==-1){ // skip blank point
                continue;
            }
            if(i==0){
                startX = startPos[0];
                startY = startPos[1];
            }else{
                startX = order[i-1][0];
                startY = order[i-1][1];
            }
            
            int curSteps = findShortestPath(map, startX, startY, destX, destY, Integer.MAX_VALUE, 0, false);
            totalSteps += curSteps;
            if(blankMap[destY][destX]=='M'){ // if is microwave
                tipSteps = totalSteps/2;
            }else{  // if is delivery
                tipSteps+=curSteps;
                curTips = Character.getNumericValue(blankMap[destY][destX]) - tipSteps;
                if (curTips>=0){
                    curTips*=10;
                }
                totalTips += curTips;
            }
        }
        result[0] = totalTips;
        result[1] = totalSteps;
        return result;
    }    
    
    /**
     * updateBestOrderLevel3
     * Determines whether the order should be updated as the best order, in terms of total steps
     * @param order, a 2D int array that is used to compare with the current best order
     * @return nothing
     */
    public static void updateBestOrderLevel3(int[][] order){
        int steps = countSteps(order);
        if(steps<bestSteps){
            bestSteps = steps;
            bestOrder = new int[order.length][2];
            for(int i = 0; i<order.length; i++){
                bestOrder[i][0] = order[i][0];
                bestOrder[i][1] = order[i][1];
            }
        }
    }  
    
    /**
     * countSteps 
     * Determines the total steps of a given order of delivery locations
     * Does not consider tips
     * @param order, a 2D int array that is used to calculate steps
     * @return total, an int that stores the total steps
     */
    public static int countSteps(int[][] order){
        int total = findShortestPath(map, startPos[0], startPos[1], order[0][0], order[0][1], Integer.MAX_VALUE, 0,false);        
        for(int i=0;i<order.length-1;i++){
            total += findShortestPath(map, order[i][0], order[i][1], order[i+1][0], order[i+1][1], Integer.MAX_VALUE, 0,false);
        }
        return total;
    }
    
    /**
     * recordPath 
     * Record a specified path to a solution array
     * @param source, a 2D char array that contains the path
     * @param dest, a 2D char array that has the path recorded
     * @return dest, a 2D char array that has the path recorded
     */
    public static char[][] recordPath(char[][] source, char[][] dest){
        for(int i=0;i<source.length;i++){
            for(int j=0;j<source[0].length;j++){
                if( isDelivery(blankMap[i][j])){ 
                    dest[i][j]='X';
                }else if(source[i][j] == 'x' && blankMap[i][j] == ' '){
                    dest[i][j]='x';
                }
            }
        }
        return dest;
    }

    /**
     * findShortestPath 
     * Finds the shortest path between two given coordinates
     * @param map, a 2D char array that contains the information of the map used
     * @param curX, x value of the starting location
     * @param curY, y value of the starting location
     * @param destX, x value of the destination
     * @param destY, y value of the destination
     * @param minDist, an int representing the total and minimum steps of the path
     * @param record, a boolean variable that determines whether the path found should be recorded to an array
     * @return minDist, an int representing the total and minimum steps of the path
     */    
    public static int findShortestPath(char[][] map, int curX, int curY, int destX, int destY, int minDist, int curDist, boolean record){
        if ((curX==destX) && (curY==destY)){
            if(curDist<minDist){
                if(record){
                    path = clearPath(path);
                    path = recordPath(map,path);
                }
                return curDist;
            }else{
                return minDist;
            }
        }else if(map[curY][curX]=='#'){
            return Integer.MAX_VALUE;
        }else{
            if(map[curY][curX]!='S'){
                map[curY][curX] = 'x';
            }else if(isDelivery(map[curY][curX])){
                map[curY][curX] = 'X';
            }
            if (isValidMove(map,curX,curY+1)) { // go 1 block down
                minDist = findShortestPath(map, curX, curY+1, destX, destY, minDist, curDist+1,record);
            }
            if (isValidMove(map,curX+1,curY)) { // go 1 block to the right
                minDist = findShortestPath(map, curX+1, curY, destX, destY, minDist, curDist+1,record);
            }
            if (isValidMove(map,curX,curY-1)) { // go 1 block up
                minDist = findShortestPath(map, curX, curY-1, destX, destY, minDist, curDist+1,record);
            }
            if (isValidMove(map,curX-1,curY)) { // go 1 block to the left
                minDist = findShortestPath(map, curX-1, curY, destX, destY, minDist, curDist+1,record);
            }
            
            // remove the 'x' marked at map[y][x] to backtrack
            map[curY][curX] = ' ';
            return minDist;
        }
    }
    
    /**
     * isDelivery 
     * Determines if the given value represents a delivery location
     * @param block, a char value that is being assessed for whether it is a delivery location
     * @return boolean, if is delivery location return true, otherwise return false
     */  
    public static boolean isDelivery(char block){
        int blockAscii = block;
        return (blockAscii>=48 && blockAscii<=57 );  //ascii values for '0'-'9'
    }    
    
    /**
     * isValidMove 
     * Determines if the given location is visitable for duber
     * @param map, a 2D char array that contains the map that will be used
     * @param x, an int that represents the x value of the specified location
     * @param y, an int that represents the y value of the specified location
     * @return boolean, if the location is a block of wall or a place that has been to before or
     *                  out of the bound of the map, return false; otherwise, return true
     */
    public static boolean isValidMove(char[][]map, int x, int y){
        if( y>=map.length || y<0 || x>=map[0].length || x<0 ){
            return false;
        }else if(map[y][x]=='#' || map[y][x]=='x' || map[y][x]=='X' || map[y][x]=='S'){
            return false;
        }else{
            return true;
        }
    }    
    
    /**
     * isInOrder 
     * Determines if the given coordinate exists in the order array
     * @param pos, an int array that contains x and y value of the coordinate
     * @param order, a 2d int array that contains a series of coordinates
     * @return boolean, if pos is in order, return true; otherwise, return false
     */    
    public static boolean isInOrder(int[] pos, int[][] order){
        for (int i = 0; i < order.length; i++) {
            if( (pos[0]==order[i][0]) && (pos[1]==order[i][1])){
                return true;
            }
        }
        return false;
    }
   
    /**
     * addToOrder 
     * Adds the given coordinate to the order array
     * @param pos, an int array that contains x and y value of the coordinate
     * @param order, a 2d int array that contains a series of coordinates
     * @return newOrder, a 2D int array that has pos added to order
     */       
    public static int[][] addToOrder(int[] pos, int[][] order){
        int[][] newOrder = new int[order.length][2];
        boolean added = false;
        for(int j = 0; j<order.length; j++) {
            if(order[j][0]==-1 && order[j][1]==-1){
                if(added == false){
                   newOrder[j][0] = pos[0];
                   newOrder[j][1] = pos[1];
                   added = true;
                   continue;
                }
            }
            newOrder[j][0] = order[j][0];
            newOrder[j][1] = order[j][1];
        }
        return newOrder;
    }    
    
    /**
     * clearPath 
     * Removes the 'x' marks in the given array
     * @param dest, a 2D char array that contains the original version of the array
     * @return dest, a 2D char array that stores the modified version of the original
     */   
    public static char[][] clearPath(char[][] dest){
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                if(dest[i][j]=='x'){
                    dest[i][j]=' ';
                }
            }
        }
        return dest;
    }    
    
    /**
     * readMap 
     * Reads the specified file and returns the map in a 2D char array format
     * @param fileName, a String that represents the name of the file that will be read by the Scanner
     * @return map, 2 2D char array that contains the information of the map taken from the given file
     */   
    public static char[][] readMap(String fileName)throws Exception{
        Scanner input = new Scanner(new File(fileName));
        int rows = input.nextInt(), cols = input.nextInt();
        char[][] map = new char[rows][cols];
        input.nextLine(); // nextLine blues - consume new line left over
        for(int i=0;i<rows;i++){
            String line = input.nextLine();
            for(int j=0;j<cols;j++){
                map[i][j] = line.charAt(j);
            }
        }
        return map;
    }
    
    /**
     * findStart 
     * Finds and returns the start(S) position; format: int[]{x,y}
     * @param map, a 2D char array that contains the information of the map used
     * @return startPos, a int array containing the x and y value of the starting point
     */
    public static int[] findStart(char[][] map){
        int[] startPos = new int[2];
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                if(map[i][j]=='S'){
                    startPos[0] = j;
                    startPos[1] = i;
                    return startPos;
                }
            }
        }
        return startPos;
    }
    
    /**
     * findDeliveries 
     * Finds and returns the coordinates of the deliveries, in a 2D int array format
     * @param map, a 2D char array that contains the information of the map used
     * @return startPos, a int array containing the x and y value of the starting point
     */
    public static int[][] findDeliveries(char[][] map){
        int total=0;
        int count=0;
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                if(isDelivery(map[i][j])){
                    total++;
                }
            }
        }
        int[][] deliveries = new int[total][2];
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                if(isDelivery(map[i][j])){
                    deliveries[count] = new int[]{j,i};
                    count++;
                }
            }
        }
        return deliveries;
    }
    
    /**
     * findVisitables 
     * Finds and returns the coordinates of the visitable blocks (deliveries and microwaves), in a 2D int array format
     * @param map, a 2D char array that contains the information of the map used
     * @return visitables, a 2D int array containing the coordinates of all deliveries and microwaves
     */
    public static int[][] findVisitables(char[][] map){
        int total=0;
        int count=0;
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                if(map[i][j]=='M' || isDelivery(map[i][j])){
                    total++;
                }
            }
        }
        int[][] visitables = new int[total][2];
        for(int i=0;i<map.length;i++){
            for(int j=0;j<map[0].length;j++){
                if(map[i][j]=='M' || isDelivery(map[i][j])){
                    visitables[count] = new int[]{j,i};
                    count++;
                }
            }
        }
        return visitables;
    }   
    
    /**
     * print2dCharArr 
     * prints the given 2d char array to the console
     * @param arr, a 2D char array that will be printed out to the console
     * @return nothing
     */
    public static void print2dCharArr(char[][] arr){
        for(int i=0;i<arr.length;i++){
            System.out.println(arr[i]);
        }
    }
    
    /**
     * outputPPM 
     * Outputs the solution in ppm image format
     * @param fileName, a String that represents the name of the file
     * @param solution, a 2D char array that contains the path of the solution, which would be outputted to the ppm image
     * @param scale, an int that determines (approximately) the width of the image, in pixels
     * @return nothing
     */
    public static void outputPPM(String fileName, char[][] solution, int scale)throws Exception{
        File ppmFile = new File(fileName); 
        PrintWriter output = new PrintWriter(fileName);
        int rows, cols;
        int sideLength = scale/path.length;
        rows = sideLength*path.length;
        cols = sideLength*path[0].length;
        
        output.println("P3"+'\n'+cols+' '+rows+'\n'+255);
        for(int i=0;i<solution.length;i++){
            String line = "";
            for(int j=0;j<solution[0].length;j++){
                String colorString = colorCodeOf(solution[i][j])+'\t';
                for(int n=0;n<sideLength;n++){
                    line += colorString;
                }
            }
            for(int n=0;n<sideLength;n++){
                output.println(line);
            }
        }
        output.close();
    }
        
    /**
     * colorCodeOf 
     * returns the color code of the given character
     * @param block, a char that will be determined for its color code
     * @return String colorStr, the color code of the specified block (format: "r g b")
     */
    public static String colorCodeOf(char block){
        String colorStr;
        
        // black for walls, white for spaces,  red for path, green for start,
        // blue for each delivery location, yellow for microwaves
        if(block=='#'){
            colorStr = "0 0 0";
        }else if(block == ' '){
            colorStr = "255 255 255";
        }else if(block == 'x'){
            colorStr = "255 0 0";
        }else if(block == 'S'){
            colorStr = "0 255 0";
        }else if(block == 'M'){
            colorStr = "255 255 0";
        }else{
            colorStr = "0 0 255";
        }
        return colorStr;
    }
    
}