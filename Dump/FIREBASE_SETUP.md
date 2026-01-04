# Firebase Firestore Setup Instructions

## 1. Firebase Console me Firestore Rules Deploy karo

### Option A: Firebase Console se (Recommended)
1. https://console.firebase.google.com/ par jao
2. Apna project "aido-it" select karo
3. Left sidebar me "Firestore Database" par click karo
4. "Rules" tab par jao
5. Niche diye gaye rules copy karke paste karo:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Marketplace preprompts collection
    match /marketplace_preprompts/{prepromptId} {
      // Anyone can read preprompts
      allow read: if true;
      
      // Only authenticated users can create preprompts
      allow create: if request.auth != null
                    && request.resource.data.authorId == request.auth.uid
                    && request.resource.data.trigger is string
                    && request.resource.data.instruction is string;
      
      // Users can only update their own preprompts
      allow update: if request.auth != null
                    && resource.data.authorId == request.auth.uid;
      
      // Users can only delete their own preprompts
      allow delete: if request.auth != null
                    && resource.data.authorId == request.auth.uid;
    }
    
    // User profiles collection
    match /users/{userId} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.auth.uid == userId;
    }
    
    // Favorites collection
    match /users/{userId}/favorites/{prepromptId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == userId;
    }
    
    // Ratings collection
    match /ratings/{ratingId} {
      allow read: if true;
      allow create, update: if request.auth != null
                            && request.resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null
                    && resource.data.userId == request.auth.uid;
    }
  }
}
```

6. "Publish" button par click karo

### Option B: Firebase CLI se (Advanced)
```bash
# Install Firebase CLI (agar nahi hai)
npm install -g firebase-tools

# Login karo
firebase login

# Project initialize karo
firebase init firestore

# Rules deploy karo
firebase deploy --only firestore:rules
```

## 2. Anonymous Authentication Enable karo (Temporary)

Abhi proper authentication nahi hai, toh anonymous auth enable karo:

1. Firebase Console me jao
2. "Authentication" par click karo
3. "Sign-in method" tab par jao
4. "Anonymous" enable karo aur save karo

Yeh temporary solution hai. Baad me proper Google/Email authentication add karenge.

## 3. Firestore Database Create karo (Agar nahi hai)

1. Firebase Console me "Firestore Database" par jao
2. "Create database" par click karo
3. "Start in production mode" select karo (rules already secure hain)
4. Location select karo (us-central1 ya asia-south1)
5. "Enable" par click karo

## 4. Test karo

App run karo aur:
1. Marketplace me jao
2. Koi preprompt upload karo
3. App close karke dobara kholo
4. Check karo ki preprompt wahi dikh raha hai

## Firestore Collections Structure

```
marketplace_preprompts/
  └── {prepromptId}/
      ├── id: string
      ├── trigger: string
      ├── instruction: string
      ├── example: string
      ├── category: string
      ├── authorId: string
      ├── authorName: string
      ├── description: string
      ├── tags: array
      ├── downloads: number
      ├── rating: number
      ├── ratingCount: number
      ├── createdAt: timestamp
      ├── updatedAt: timestamp
      ├── isFeatured: boolean
      └── isVerified: boolean

users/
  └── {userId}/
      ├── username: string
      ├── email: string
      └── favorites/
          └── {prepromptId}/
              └── addedAt: timestamp

ratings/
  └── {ratingId}/
      ├── userId: string
      ├── prepromptId: string
      ├── rating: number
      ├── review: string
      └── createdAt: timestamp
```

## Current Status

✅ Firebase SDK added to project
✅ google-services.json configured
✅ Firestore implementation complete
✅ Security rules created
⏳ Need to deploy rules to Firebase Console
⏳ Need to enable Anonymous Auth
⏳ Need to create Firestore database (if not exists)

## Next Steps

1. Firebase Console me rules deploy karo (upar instructions follow karo)
2. Anonymous authentication enable karo
3. Firestore database create karo
4. App test karo - data persist hoga!
