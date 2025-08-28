# AI-BASED-RECOMMENDATION-SYSTEM



*COMPANY*: CODETECH IT SOLUTIONS

*NAME*: SHAIK RAYEES

*INTERN ID*: CT04DY392

*DOMAIN*: JAVA PROGRAMMING

*DURATION*: 4 WEEKS

*MENTOR*: NEELA SANTHOSH KUMAR



# DESCRIPTON

## 🎯 AI-Based Recommendation System (Java)

This project implements an **AI-based Recommendation System** using **Java**.  
It is a **user-based collaborative filtering engine** that suggests products or content to users based on their **preferences** and the behavior of **similar users**.

---

### 🚀 Features
- Built in **pure Java** (single-file program, no external dependencies).  
- Implements **User-Based Collaborative Filtering** with **Cosine Similarity**.  
- Finds **Top-K nearest neighbors** for a user.  
- Predicts unseen ratings using **weighted average of neighbors**.  
- Generates **Top-N recommendations** for the target user.  
- Includes a **fallback popular-items recommender** if not enough overlap exists.  
- Auto-generates a **sample dataset (`sample_ratings.csv`)** on first run.

---

### 🛠️ Tech Stack
- **Language**: Java  
- **Libraries**: None (dependency-free)  
- **Algorithm**: Collaborative Filtering (Cosine Similarity, k-NN)  

---

### 📂 Project Structure

RecommenderSingleFile.java # Main program
sample_ratings.csv # Auto-generated dataset (user, item, rating)


---

### 📊 Sample Dataset
The program auto-creates a `sample_ratings.csv` file if missing.

''csv
userId,itemId,rating
U1,I1,5
U1,I2,4
U1,I3,2
U1,I4,1
U2,I1,5
U2,I2,5
U2,I3,1
U2,I5,4
U3,I2,4
U3,I3,5
U3,I4,2
U3,I6,5
...




### ▶️ How to Run
Step 1: Compile
javac RecommenderSingleFile.java

Step 2: Run
java RecommenderSingleFile [targetUserId] [kNeighbors] [topN] [dataFile]


targetUserId → User for whom recommendations are generated (default = U1)

kNeighbors → Number of neighbors to consider (default = 3)

topN → Number of recommendations (default = 5)

dataFile → Path to ratings CSV file (default = sample_ratings.csv)

📌 Example Run
java RecommenderSingleFile U3 3 5

### ✅ Output
===== AI-Based Recommendation System (User-Based CF) =====
Target User : U3
Neighbors K : 3
Top-N       : 5
Data File   : sample_ratings.csv

--- Top Neighbors for U3 ---
1) U5      similarity = 0.7642
2) U2      similarity = 0.6128
3) U1      similarity = 0.4025

--- Top Recommendations for U3 ---
1) I5      predictedScore = 4.612
2) I8      predictedScore = 4.205







### 🔍 How It Works

Load dataset (userId,itemId,rating).

Compute cosine similarity between the target user and all other users.

Select Top-K neighbors (most similar users).


Recommend Top-N items with the highest predicted scores.

If no overlap exists → fall back to most popular items.

### 📚 Applications

E-commerce: Product suggestions (Amazon, Flipkart)

Streaming Platforms: Movie & music recommendations (Netflix, Spotify)

Content Platforms: Article/video recommendations (YouTube, Medium)

## 🧑‍💻 Author

Name: **SHAIK RAYEES**

Internship Task: **AI-Based Recommendation System using Java**

Completion: **✅ Working recommendation engine with sample dataset**






## OUTPUT


<img width="1920" height="1020" alt="image" src="https://github.com/user-attachments/assets/8b807002-1fb5-44a2-be41-1de4e4c0105f" />





