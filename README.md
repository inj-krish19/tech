# 🚀 Tech2Xplore

**Tech2Xplore** is a domain-specific, full-stack web application designed for developers, researchers, and tech enthusiasts to share, discover, and interact with in-depth technical content.

Built as part of academics, it solves the core issue of fragmented and non-specialized blogging platforms by providing a structured, community-driven ecosystem for high-quality tech content.

---

## 🧠 Purpose

Unlike general-purpose platforms like Medium or LinkedIn, Tech2Xplore focuses **solely on the tech domain**—offering:
- Well-categorized, searchable content
- Rich formatting for technical writing
- Community interaction via likes, comments, and follows
- Author collaboration and group publishing

---

## 🎯 Features

### 👤 User-Centric
- Role-based authentication (User/Admin)
- Blogger profile with bio, social links, and dashboard
- Follow/Followers system

### ✍️ Content Creation
- Rich-text blog editor with code snippets, media, and previews
- Categorization (e.g., AI, Cybersecurity) and tagging (e.g., Python, Java)
- Post drafts, publish, edit, delete

### 💬 Engagement
- Like, comment, and threaded replies
- Real-time notifications
- Community modules (join, create, post)

### 🔍 Discoverability
- Advanced search (by keyword, title, author, category)
- Personalized recommendation engine
- Trending posts & post rankings

### 🛡️ Moderation
- Admin panel to flag, approve, and manage posts/users
- Category & keyword taxonomy control
- Secure Spring Boot-based API architecture

---

## 🛠️ Tech Stack

| Layer          | Technology                                  |
|----------------|---------------------------------------------|
| Frontend       | HTML, CSS, JavaScript, Thymeleaf, Bootstrap |
| Backend        | Java 17+, Spring Boot, Spring Security      |
| Database       | PostgreSQL / MySQL                          |
| Deployment     | Render, Railway, Neon, Aiven                |
| Version Control| Git, GitHub                                 |
| Hosting Tools  | Docker, Railway, CI/CD (GitHub Actions)     |

---

## 🧱 Database Design

The system is split into:

### **Entity Tables:**
- `authors`: Blogger profiles
- `articles`: Blog posts
- `topics`: High-level domains
- `keywords`: Granular tags
- `groups`: Community spaces

### **Relational/Transactional Tables:**
- `post_comments`: Comments and replies
- `post_interactions`: Likes/dislikes
- `connections`: Follow system
- `collaborations`: Co-authored posts
- `memberships`: Blogger ↔ Community relation
- `keyword_assignments`: Post ↔ Keyword mapping
- `post_category_assignments`: Post ↔ Topic mapping
- `suggestions`: Feedback system

---

## 🧪 Testing Highlights

- 30+ test cases passed (authentication, CRUD, XSS/SQL injection resistance)
- Responsive UI (desktop & mobile)
- All user interactions reflected in dashboard metrics
- Admin tools validated (flag/delete/approve)
- Basic rate-limiting and input sanitization enabled

---

## 💡 Future Enhancements

> These are under active planning:

- 🔗 GitHub/LinkedIn OAuth login
- 🤝 Real-time collaborative post editing
- 📈 Post engagement analytics dashboard
- 🧠 AI-based post recommendations & content summarization
- 📱 Mobile app / Progressive Web App (PWA) version
- 🏆 Gamification (badges, levels, contribution streaks)
- 🧠 Spam detection and plagiarism scanner

---

## 🧰 Setup Instructions

```bash
# Clone repo
git clone https://github.com/inj-krish19/tech.git
cd tech

# Backend setup
cd backend
cp .env.example .env
./mvnw spring-boot:run

