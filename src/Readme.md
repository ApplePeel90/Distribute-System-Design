1. Copy the code to csgrads1 server

   ```powershell
   scp Message.java ServerTest.java configuration.txt launcher.sh cleanup.sh netid@csgrads1:/PROJECT_DIR
   ```

2. Open ServerTest.java and update the file path of configuration.txt

3. Compile java file

   ```powershell
   javac *.java
   ```

4. Run launcher.sh

   ```powershell
   ./launcher.sh
   ```

5. After testing, run cleanup.sh to kill all the process

   ``` spreadsheet
   ./cleanup.sh
   ```

   

