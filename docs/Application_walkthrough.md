<div align="center">
    <h1>Application walkthrough</h1>
</div>

This is a comprehensive user guide for BriefAI. This document will walk you through the user interface, key features, and step by step workflows to help navigate through the application.

## Step by step feature guide

### 1. Login

![Login screen](/docs/images/app_screenshots/Login_screen.png)

**Purpose**:
Login to application using user credentials

**Instructions**:

- Enter email and password, then click on 'sign in'.

**Features**:

- Frontend email validation (Email has to be in `name@domain` format)
- Verifies credentials against user account present in the system.
- In case invalid credentials it shows error and in case of valid credentials it logs you in.
- If you don't have any user account then you casn use `Create account` link to sign up.

### 2. New user creation

![New user creation screen](/docs/images/app_screenshots/New_user_creation_screen.png)

**Purpose**:
Create a new user

**Instructions**:

- Enter name, email and password, then click on 'create account'.

**Features**:

- Validations on name, email and password.
- Does not allow multiple accounts with same email.
- If you already have an account then use `Sign in` link to login.

### 3. Account verification using email OTP

![Account verification using email OPT](/docs/images/app_screenshots/Account_verification_screen.png)

**Purpose**:
Verify user account/email

**Instructions**:

- Enter the OTP received on provided email and click on `verify email` button.

**Features**:

- If OTP is incorrect then it will show the error message.
- Every OTP is valid for 5 min.
- Only 5 incorrect OTP attempts are allowed after that the OTP becomes invalid even though it is still within 5 min limit.
- To use resend OTP you have to wait a cooldown period of 1 min, post that only new OTP will be shared.
- In case of resend OTP is clicked previous OTP automatically becomes invalid ensuring that at a time only 1 valid OTP is available.
- On successful verification, you will receive success message on screen and you will be redirected to login screen.

Below is the example of email you will receive. (email id will be different based on environment variables you provided in `.env` file)

![Sample OTP email](/docs/images/app_screenshots/Example_OTP_email.png)

### 4. Main homepage

![Main homepage (documents)](/docs/images/app_screenshots/Main_homepage.png)

**Purpose**:
Display summery of documents uploaded and list down all uploaded documents.<br>
We can proces, delete or upload new documents as well from same screen.

**Instructions**:

- Click on `Upload document` button on top right corner to upload your first document.
- Select .pdf, .doc or .docx document and upload.

**Features**:

- Op top the screen shows overview of documents i.e. total documents uploaded by user, total documents ready to answer user questions, etc.
- Under document library section all documents uploaded by user will be displayed.
- Just after upload the document status is `Uploaded` with two options, `Process` and `Delete`.
- Delete simply remove that document from the syatem and process initiate the document processing pipeline for that document.
- Once we start processing of any document status of that document changes to `Processing`.
- In case of successful processing the document status changes to `Ready` and we can chat with the document.

See below image which shows different states of document and options we have in those states.

![Document dashboard](/docs/images/app_screenshots/Document_dashboard.png)

### 5. New conversation screen

![Start new conversation](/docs/images/app_screenshots/New_conversation_screen.png)

**Purpose**:
Start new conversation. As the application support context aware response, we can have separate conversation about separate topics.

**Instructions**:

- Enter a short name for the conversation and click on `Create conversation`.
- This will create the conversation and take you to the chat window.

**Features**:

- All conversations are isolated form each other.
- Inside the conversation context aware response is supported. Means you can ask question based on previous response. (see below screenshot)

In below screenshot the application replied with the appropriate response to the provided question based on document content only. It also provide the citation as well.
![Question_answer](/docs/images/app_screenshots/Question.png)

Now in below image we are asking the followup question which need the model to be aware about the context, because without context the
'third principle' will not make any sense to the model.<br>
Here in below screenshot we can see that the application correctly understood the third principle means user is talking about 'Polymorphism' and answered the question accordingly.

![Context aware question](/docs/images/app_screenshots/Context_aware_question.png)

- In case of unrelated question or any question which can not be answered based on given document then the application does not invent anything on it's own, it stick to the document and respond saying the question can not be answered based on provided document.

![Unrelated question response](/docs/images/app_screenshots/Unrelated_question_response.png)

- It also has multilanguage support

![Hindi conversation](/docs/images/app_screenshots/Hindi_conversation.png)
![Spanish conversation](/docs/images/app_screenshots/Spanish_conversation.png)

### 6. Chat dashboard

![Chat dashboard](/docs/images/app_screenshots/Chat_dashboard.png)

**Purpose**:

- To listdown and manage chats
- Isolate conversations on different topics

**Features**:

- This dashboard provide list of all conversations you have created.
- On click of any conversation it opens the conversation and display all messages from that conversatio.
- User can continue the same conversation in the chat window.
