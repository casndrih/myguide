# MyGuide
**MyGuide project is currently under development on new features, bug fixes, clean code and further improvements!
By <a href="https://www.linkedin.com/in/casper-n-drih-722482184">**Casper N'drih**</a> and <a href="https://www.linkedin.com/in/isaac-kialo-jr-silas">**Isaac Silas**</a>.**

# Description
**MyGuide is a Web Admin controlled Android app which we have developed as part of our project at the 2021 Apec App Challenge. The App strives to provide the best places for Tourism exploration in Papua New Guinea based on different categories as well as Safe travel and Flight booking, Detailed guides and Navigation, Weather forecast and Covid-19 National news. It also supports Firebase Notification(GCM) and Analytics to measure interactions and inform users whenever a place has been added or updated.**

# Screenshots
<div align="center"> <img src="/screenshot1.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screenshot5.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screenshot3.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screenshot2.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screemshot4.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screenshot6.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screenshot7.jpg" width="400px"</img> </div>
<div align="center"> <img src="/screenshot8.jpg" width="400px"</img> </div>

# Android project structure
(***Folders organize by their function***)

**All Activity Class can be found in com.casper.myguide**

**Adapter for Gridview, Image List placed on com.casper.myguide.adapter**

**All class related REST API request on com.casper.myguide.connection**

**The supporting data like, constant, database com.casper.myguide.data**

**The weather supporting data like, constant, utility classes
com.casper.myguide.weatherutils**

**Fragment page placed on com.casper.myguide.fragment**

**Fcm notification Handler on com.casper.myguide.fcm**

**All Object Model placed on com.casper.myguide.model**

**Weather Object Model placed on com.casper.myguide.weathermodel**

**Internet detector, callback class, and snippet com.casper.myguide.utils**

**Custom widget class placed on com.casper.myguide.widget**


# Web admin structure
(***Folders organize by their function***)

**1  parent/**

**2  +--- app/ # This folder contains all js controller**

**3  +---+--- controller/ # contains controller for page**

**4  +---+--- services/ # api and database transaction**

**5  +---+--- uploader/ # image file uploader**

**6**

**7  +--- css/ # css style**

**8  +--- js/ # javascript library files**

**9**

**10 +--- templates/ # contains html view**

**11**

**12 +--- uploads/ # location for uploaded image**

**13 +--- index.html**
