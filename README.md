**COME AI Mobile App Documentation**

**📱 Overview**

The COME AI mobile app enables volunteers to assist households by:

- Registering them

- Verifying membership

- Filling out a multi-language questionnaire (English/Assamese)

- Supporting offline data collection and auto-sync when back online

**🔄 Application Flow**

LoginFragment 
   ↓ 
MembershipFragment 
   ├──> QuestionnaireFragment (if membership exists)
   └──> RegisterFragment → QuestionnaireFragment (if not found)

**📦 Module Responsibilities**

**🔐 LoginFragment**

Purpose: Handles volunteer login using phone number.

Sends to Lambda:
{
  "action": "volunteer_login"
}
Navigates to: MembershipFragment, 
passing:
- volunteer_phone_number via Bundle

🧾 MembershipFragment
Purpose:
- Verify household membership ID
- Navigate to RegisterFragment if membership is not found
Sends to Lambda:
{
  "action": "check_membership"
}
Navigation:
- If membership exists → QuestionnaireFragment
- If not found → RegisterFragment
Passes via Bundle:
- membership_id
- volunteer_phone_number

**🏠 RegisterFragment**

Purpose: Register a new household and members.

Sends to Lambda:
{
  "action": "register_household",
  "membership_id": "...",
  "volunteer_phone_number": "...",
  "city_or_village": "...",
  "pincode": "...",
  "members": [
    { "age": ..., "gender": "..." },
    ...
  ]
}

Backend: Lambda fetches volunteer_id based on phone number and stores all data in the database (MySQL on RDS).

**📝 QuestionnaireFragment**

Purpose: Display and collect questionnaire responses.

Key Features:

- Shows 10 questions per page
- Toggle between English and Assamese
- Accepts user input
- Stores responses in offline_responses.json if no internet
- Automatically syncs in onResume() when back online
Uses:

- questions.json from assets/
Sends to Lambda: JSON containing all answers

**☁️ AWS Lambda (Backend)**

Lambda verifies and stores data into MySQL (RDS) and handles the following actions:
- volunteer_login
- check_membership
- register_household
- submit_questionnaire

**📁 JSON Assets**

- questions.json located in assets/

  - Contains questionnaire in English and Assamese

**🌐 Offline Support**

- Responses saved to: offline_responses.json if offline
- When internet is available:
  - Data is synced automatically
  - File is deleted upon successful submission
