# Green Pass: PDF Reader (unofficial) Android App
## Offers a convenient access to a single one-sided PDF document and its QR code (if present in the document) like e.g. an EU Digital COVID Certificate
[Download in Google Play Store](https://play.google.com/store/apps/details?id=com.michaeltroger.gruenerpass) 

<b>Unofficial app that is fully open source. One one-sided PDF can be imported. If you require to import more than that, then please go for one of the other available solutions. This app is basically a super simple PDF reader for one-sided documents, with convenient fullscreen QR code rendering (if present in the PDF).</b>

<b>Brief Instruction App Usage</b>
- Open App
- Import PDF
- Done

<b>Brief Instruction  App Usage 2</b>
- Open File browser, Email, Browser, Adobe Reader, or a similar app
- Choose to open/share the PDF with "Green Pass" 
- Done

<b>Description</b>
Very simple app, that does nothing more than conveniently displaying the "Green Pass" certificate, from a given PDF file. The PDF has to be present already. In Austria the Green Pass is a one-sided PDF document with QR code (EU Digital COVID Certificate) that can be downloaded from https://gesundheit.gv.at The reader was optimized for this use-case (display of this PDF and its QR code). Other one-sided PDF documents from other authorities should generally work too though, since this app follows a universal approach. 

<b>Functionality</b>
The Green Pass PDF (EU Digital COVID Certificate) file that you downloaded from https://gesundheit.gv.at or your local authority has to be imported into the app. Following the QR code is displayed in fullscreen (if part of the provided PDF), as well as the PDF itself. That's already it. This app assumes you have the Green Pass PDF certificate (EU Digital COVID Certificate) from gesundheit.gv.at already on your phone. If there is no QR code detected in your PDF, then only the PDF is rendered.

<b>More Features</b>
- PDFs can be opened and shared from other apps
- Dark Mode support
- Supports also password protected PDFs

<b>Does one require this app?</b>
Not really. For the Green Pass PDF from gesundheit.gv.at any casual PDF-Reader is completely sufficient. The use case for this app is solely the improved usability. One saves the time for searching the PDF and zooming in for the QR code (if present in the document).

<b>Why offering this app when there is an official one?</b>
This app was created before an official app was available.

<b>Why does the fullscreen QR code look differently than in the PDF?</b>
The algorithm to display is different. For reading devices the code is identical.

<b>What is this app doing differently compared to other available approaches?</b>
One of these approaches requires the Green Pass PDF to be uploaded to a webserver. There the PDF and the QR code are read out and converted into a format, that can be used in so-called Wallet apps. The source code is not available for all solutions. In comparison to that, this app saves time, since the PDF can be imported as it is and it has not to be converted into another format. Further more the file does never leave the app on the respective device and the source code of the app is fully open source.

<b>Privacy</b>
This app doesn't need any permissions. That also means it has no access to internet etc. The document is solely copied to the app's cache and is merely read out for displaying. The document doesn't ever leave the app, stays locally and offline and can be removed from the cache by the user whenever he/she wishes. No ads, no tracking. The app is fully open source.

<img src="/screenshot.jpg" width="200"> <img src="/screenshot1.jpg" width="200"> <img src="/screenshot2.jpg" width="200">

*Kudos to:*
- *[Sebastian Schmid](https://github.com/da5ebi) - for consulting me in terms of UX*
