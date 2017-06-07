package de.unidue.continuityguide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

import de.unidue.continuityguide.Maker.Variable;


/**
 * This UI is the application entry point. A UI may either represent a browser window 
 * (or tab) or some part of a html page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be 
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Theme("mytheme")
public class MyUI extends UI {
	
	private ContinuityGuide continuityguide = new ContinuityGuide();

	DocUploader receiver = new DocUploader();
	private File tempFile;

	Label pageTitle = new Label("<h1>Variable Classificator</h1>", ContentMode.HTML);
	Label varContent = new Label();
	CheckBoxGroup<String> suggestionBox = new CheckBoxGroup<>("Suggestions");
	Button indexButton = new Button("Reset Index");
	Button evalButton = new Button("Evaluate");

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		final VerticalLayout layout = new VerticalLayout();

		layout.setWidth("100%");

		Upload upload = new Upload("", receiver);
		upload.setButtonCaption("Click here to Upload/Change Variable");
		upload.setImmediateMode(true);
		upload.addSucceededListener(receiver);

		varContent.setWidth("100%");
		varContent.setVisible(false);

		suggestionBox.setVisible(false);

		indexButton.addClickListener(clickEvent -> continuityguide.makeIndex());
		evalButton.addClickListener(clickEvent -> continuityguide.evaluate());

		layout.addComponents(pageTitle, upload, varContent, suggestionBox, indexButton, evalButton);

		setContent(layout);

	}


	class DocUploader implements Receiver, SucceededListener {
		public File file;

		public OutputStream receiveUpload(String filename,
				String mimeType) {
			try {
				tempFile = File.createTempFile(filename, "xml");
				tempFile.deleteOnExit();
				return new FileOutputStream(tempFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;

		}

		public void uploadSucceeded(SucceededEvent event) {
			List<String> suggestions = continuityguide.classify(tempFile.getPath());
			
			if (suggestions != null) {
				Variable v = Maker.parse(tempFile.getPath());
				fillVarContent(v);

				suggestionBox.setItems(suggestions);

				suggestionBox.setVisible(true);
			} else {
				Notification.show("Error",
						"That's not a Variable XML file.",
						Notification.Type.ERROR_MESSAGE);
			}
		}
	};

	private void fillVarContent(Variable v) {
		varContent.setValue( 
				"<h2>Variable:</h2>" +
						"<b>ID: </b>" + v.id + "<br>" +
						"<b>Studie: </b>" + v.studyno + "<br>" +
						"<b>VarNo: </b>" + v.varino + "<br>" +
						"<b>Label: </b>" + v.label + "<br>" +
						"<b>Fragetext: </b>" + v.qtext + "<br>"
				);
		varContent.setContentMode(ContentMode.HTML);

		varContent.setVisible(true);
	}



	@WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
	public static class MyUIServlet extends VaadinServlet {
	}
}
