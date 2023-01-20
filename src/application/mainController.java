package application;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class mainController{

	//Java fx core elements
	private Parent root;
	private Stage stage;
	private Scene scene;


	//Injections
	@FXML
	private Button placePolicyButton, extractButton, openPolicyButton, unzipNextButton, directoryNextButton, beginButton;

	@FXML
	private Label errorPolicyLabel, specialtyLabel, QLSTLabel, beginCompareLabel, comparisonFilenameLabel, updatedFilenameLabel, zipErrorLabel,
	directoryLabel, directoryErrorLabel;	
	@FXML
	private ChoiceBox<String> specialtyBox, QLSTBox;

	//variables
	private File file;
	private String ext;
	private String specialty[] = {"specialty", "nonspecialty"};
	private String qlst[] = {"QL", "ST"};
	private int count;
	private boolean exit = false;


	//***********************************************  INTRO PROMPT *****************************************//

	//The introduction prompt will ask the user whether the new, updated policies are located in a zip folder or not.

	//function will pass control the unzipper functions if it is zipped
	public void yesZip(ActionEvent e) throws IOException {
		System.out.println("YES");

		root = FXMLLoader.load(getClass().getResource("zip.fxml"));
		stage = (Stage)((Node)e.getSource()).getScene().getWindow();
		scene = new Scene(root, 400, 350);
		stage.setScene(scene);
		stage.show();
	}

	//function will pass control straight to the directoryChooser
	public void noFolder(ActionEvent e) throws IOException {
		System.out.println("No");
		root = FXMLLoader.load(getClass().getResource("directory.fxml"));
		stage = (Stage)((Node)e.getSource()).getScene().getWindow();
		scene = new Scene(root, 400, 350);
		stage.setScene(scene);
		stage.show();

	}

	//*********************************************** ZIP UPLOAD *****************************************//

	//function will upload a zip file and initiate extraction of the zip
	public void upload(ActionEvent e) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Zip File");

		try {

			//filechooser to choose the zip file
			file = fileChooser.showOpenDialog(stage);
			System.out.println(file.getPath());
			helperController.file=file;

			//singling out the extension to double check it is a zip
			ext = getExtensionByStringHandling(file.getPath()).get();


			//displaying the filename in the GUI
			updatedFilenameLabel.setText(file.getPath());


			//switch stage accordingly based on the extension, if ZIP, unpack. If not zip, then display error
			if(ext.toLowerCase().compareTo("zip") == 0) {
				extractButton.setVisible(true);
				zipErrorLabel.setVisible(false);
			}
			else {
				extractButton.setVisible(false);
				zipErrorLabel.setVisible(true);

			}
		} catch(Exception d) {
			System.out.println("File Selection Canceled.");
			d.printStackTrace();
		}

	}

	//helper function that will get the extension of a file
	public Optional<String> getExtensionByStringHandling(String filename) {
		return Optional.ofNullable(filename)
				.filter(f -> f.contains("."))
				.map(f -> f.substring(filename.lastIndexOf(".") + 1));
	}

	//function that will pass control to our finished zip functions, only after the file in unzipped.
	public void switchtoUnzip(ActionEvent event) throws IOException, InterruptedException {
		root = FXMLLoader.load(getClass().getResource("finishedZip.fxml"));
		stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		scene = new Scene(root, 400, 350);
		stage.setScene(scene);
		stage.show();

		unzip(file);
	}

	//*********************************************** DIRECTORY UPLOAD *****************************************//	

	//A completely different function was needed for the directory upload, since a file chooser requires a file. This function
	//will allow the user to select the already unzipped folder of the policy documents. 
	public void directoryChooser(ActionEvent e) {

		//directory chooser
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Select Unzipped Folder");
		File selectedDirectory = dc.showDialog(stage);

		//displaying the directory in the gui
		directoryLabel.setText(selectedDirectory.getPath());
		file = selectedDirectory;
		System.out.println(file);
		helperController.file=file;

		//if the folder is empty, display error and do not let proceed
		String[] files = selectedDirectory.list();

		if (files.length > 0) {
			directoryNextButton.setVisible(true);
			directoryErrorLabel.setVisible(false);
		}
		else {
			directoryErrorLabel.setVisible(true);
			directoryNextButton.setVisible(false);
		}


	}
	//***********************************************  ZIP EXTRACT *****************************************//

	//function that will unzip the zip file and place it in a directory in the same the directory the zip was located, with the same name
	public void unzip(File input) throws IOException {

		String fileZip = input.getPath();

		//remove Extension
		String withoutZip = removeExtension(input.getPath());

		File destDir = new File(withoutZip);
		helperController.file = destDir;
		System.out.println(destDir);

		byte[] buffer = new byte[1024];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {
			File newFile = newFile(destDir, zipEntry);

			if (zipEntry.isDirectory()) {
				if (!newFile.isDirectory() && !newFile.mkdirs()) {
					throw new IOException("Failed to create directory " + newFile);
				}
			} else {
				// fix for Windows-created archives
				File parent = newFile.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory " + parent);
				}

				// write file content
				System.out.println(newFile.getName());
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zipEntry = zis.getNextEntry();        
		}

		zis.closeEntry();
		zis.close();


	}
	//helper function that will create the new file
	public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	//helper function to remove the extension on a file
	public static String removeExtension(final String s)
	{
		return s != null && s.lastIndexOf(".") > 0 ? s.substring(0, s.lastIndexOf(".")) : s;
	}

	//***********************************************  COMPARISON BEGIN *****************************************//


	//function that will give control to our comparison/fileRead functions
	public void startComparison(ActionEvent event) throws IOException {


		FXMLLoader loader = new FXMLLoader(getClass().getResource("comparison.fxml"));
		root=loader.load();

		stage = (Stage)((Node)event.getSource()).getScene().getWindow();
		scene = new Scene(root, 400, 350);
		stage.setScene(scene);
		stage.show();


	}

	//***********************************************  FILEREAD  *****************************************//	

	//function that will initialize our compare
	public void buttonPress(ActionEvent e) {
		errorPolicyLabel.setVisible(true);
		placePolicyButton.setVisible(false);
		specialtyBox.getItems().addAll(specialty);
		QLSTBox.getItems().addAll(qlst);
		beginButton.setVisible(false);
		//		beginCompareLabel.setVisible(false);
		excelFileRead(helperController.file);

	}


	//funtion that will read the excel file and append information into lists so that we can compare against new policy files
	public void excelFileRead(File newPolicy) {


		try {
			//excel path
			String filepath="C:\\Users\\brend\\OneDrive\\Desktop\\unzip\\Zip and all files2.xlsx";
			helperController.excelPath = filepath;
			File excel = new File(filepath);
			FileInputStream fs = new FileInputStream(excel);

			XSSFWorkbook wb = new XSSFWorkbook(fs);
			XSSFSheet sheet = wb.getSheetAt(0);


			//two lists to store filename and specialty
			List<String> filenameList = new ArrayList<String>();
			List<String> specialtyList = new ArrayList<String>();

			//iterating over each row
			for(int rowIndex = 0; rowIndex<=sheet.getLastRowNum(); rowIndex++) {

				//grabbing entire row
				Row row = sheet.getRow(rowIndex);
				if (row!= null) {

					//filename
					Cell filenameCell = row.getCell(1);
					if(filenameCell != null) {
						String filename = filenameCell.getStringCellValue();

						//add to list
						filenameList.add(filename);
					}
					//specialty
					Cell specialtyCell = row.getCell(2);
					if(specialtyCell != null) {
						String specialty = specialtyCell.getStringCellValue();
						//add to list
						specialtyList.add(specialty);
					}
				}

			}

			//each filename must have a corresponding specialty in the excel sheet, so just making sure the size is eqal
			if(specialtyList.size() == filenameList.size()) {
				System.out.println("Success");
				comparison(filenameList, specialtyList, newPolicy);
			}
			else {
				//fix the excel sheet
				System.out.println("Error in excel sheet...");
				System.exit(0);
			}

			wb.close();

		}

		catch(Exception d) {
			d.printStackTrace();
		}
	}


	//function that will conduct the comparison between the excel sheet lists and the policy files
	public void comparison(List<String> filenameList, List<String> specialtyList, File newPolicy) throws InterruptedException, IOException {


		//created a list to keep track of the policies that were not located in the excel sheet but were a new policy
		List<String> falseList =  new ArrayList<String>();

		//iterating over the new policy files
		for(String f: newPolicy.list()) {

			//excel sheet contains updated policy ************************************** NEED TO IMPLEMENT DATE MODIFICATION
			if(filenameList.contains(f)) {
				//System.out.println(file + ": True");
			}
			else {
				//excel sheet does not contain policy
				falseList.add(f);
			}
		}
		//calling of the prompt user function with the entries that were not in the excel sheet
		promptUser(falseList);

	}

	//function that will prompt the user to enter specialty information, and QL/ST information, as well as place the new policy where
	// they would desire it to be stored. Might later asked to be automated.
	public void promptUser(List<String> falseList) throws InterruptedException, IOException {

		//intially setting ChoiceBoxes to empty
		QLSTBox.setValue("");
		specialtyBox.setValue("");

		count = -1;
		
		
		if (falseList.size() == 0) {
			beginCompareLabel.setText("No New Policies to Add!");
			errorPolicyLabel.setVisible(false);

			return;
		}
		
		//each time the specialtybox is acted on
		specialtyBox.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				//if either box is empty, display error
				if((specialtyBox.getValue().compareTo("") == 0 || QLSTBox.getValue().compareTo("") == 0)) {
					errorPolicyLabel.setVisible(true);
					placePolicyButton.setVisible(false);
				}
				else {
					errorPolicyLabel.setVisible(false);
					placePolicyButton.setVisible(true);
				}
			}
		});
		//each time the qlstbox is acted on
		QLSTBox.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				//if either box is empty, show error
				if((specialtyBox.getValue().compareTo("") == 0 || QLSTBox.getValue().compareTo("") == 0)) {
					errorPolicyLabel.setVisible(true);
					placePolicyButton.setVisible(false);

				}
				else {
					errorPolicyLabel.setVisible(false);
					placePolicyButton.setVisible(true);
				}
			}
		});

		//setting action of the place policy button
		placePolicyButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent e) {

				try {
					//setting labels and boxes to visible initially
					comparisonFilenameLabel.setVisible(true);
					specialtyLabel.setVisible(true);
					QLSTLabel.setVisible(true);
					specialtyBox.setVisible(true);
					QLSTBox.setVisible(true);
					openPolicyButton.setVisible(true);
					
					//displaying number of new policies
					beginCompareLabel.setText( falseList.size() + " New Policies Discovered");
					
					//setting the text of the comparison label
					if(count!= falseList.size() - 1)
						comparisonFilenameLabel.setText(falseList.get(count + 1));
					else {
						// when the count is at the size, settext to the count and exit
						comparisonFilenameLabel.setText(falseList.get(count));
						exit = true;
					}
					
					
					if(count != -1) 
						helperController.current = falseList.get(count);
					
					System.out.println(specialtyBox.getValue());
					System.out.println(QLSTBox.getValue());

					if(count == -1) {
						count++;
						return;
					}
					
					//directory chooser to place the new policy
					DirectoryChooser dc = new DirectoryChooser();
					
					//inital directory of the chooser
					File init = new File("C:\\Users\\brend\\OneDrive\\Desktop\\unzip");
					dc.setInitialDirectory(init);
					File destDir = dc.showDialog(stage);
					System.out.println(destDir.getPath());
					
					//constucting the complete filepath of the file
					File newFile = new File(helperController.file + File.separator + helperController.current);
					
					//using the fileutils library to copy the file to our destination directory
					FileUtils.copyFileToDirectory(newFile, destDir);
					
					FileInputStream myxls = new FileInputStream(helperController.excelPath);
					XSSFWorkbook wb = new XSSFWorkbook(myxls); 
					XSSFSheet sheet = wb.getSheetAt(0);	
					int lastRow = sheet.getLastRowNum();
					System.out.println(lastRow);
					Row row = sheet.createRow(++lastRow);
					row.createCell(1).setCellValue(helperController.current);
					row.createCell(2).setCellValue(specialtyBox.getValue());
					row.createCell(3).setCellValue(QLSTBox.getValue());
					myxls.close();
					FileOutputStream out = new FileOutputStream(new File(helperController.excelPath));
					wb.write(out);
					wb.close();
					out.close();

					//resetting choice boxes
					QLSTBox.setValue("");
					specialtyBox.setValue("");
					count++;
					
					//if exit, then exit immediately
					if(exit) {
						throw new ConditionMetException();
					}
				}

				catch(Exception d) {
					ConditionMetException.print();
					beginCompareLabel.setText("Finished adding the new policies!");
					comparisonFilenameLabel.setVisible(false);
					specialtyLabel.setVisible(false);
					QLSTLabel.setVisible(false);
					specialtyBox.setVisible(false);
					QLSTBox.setVisible(false);
					placePolicyButton.setVisible(false);
					openPolicyButton.setVisible(false);
					errorPolicyLabel.setVisible(false);
					d.printStackTrace();

				}
			}
		});
		
		//intial press of the button to intialize
		placePolicyButton.fire();
	}

	//function that allows for the file currently being processes to open from the gui
	public void openPolicy(ActionEvent e) {
		System.out.println(helperController.file);
		String[] fileList = helperController.file.list();
		for (String filename: fileList) {
			if (filename.compareTo(comparisonFilenameLabel.getText()) == 0) {
				try {
					File newFile = new File(helperController.file + File.separator + filename);
					System.out.println(newFile.getPath());
					Desktop.getDesktop().open(newFile);
					break;
				}
				catch(Exception exception) {
					exception.printStackTrace();
				}
			}
		}

	}

}