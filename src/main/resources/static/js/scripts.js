function toggleReadMore(button) {
    const parent = button.closest('.post-description');
    const moreText = parent.querySelector('.more-text');

    if (moreText.style.display === 'none' || !moreText.style.display) {
        // Show more
        moreText.style.display = 'inline';
        button.innerText = 'Read less...';
    } else {
        // Show less
        moreText.style.display = 'none';
        button.innerText = 'Read more...';
    }
}

document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.tag').forEach(function (button) {
            button.addEventListener('click', function () {
                const url = button.getAttribute('data-url');
                if( ! location.href.includes("create_post") ){	location.href = url;	}
            });
        });
    });

/* function sendRequest( other, blogger, action ){
	
	fetch(`/connection/${action}/${blogger}`, {
        method: 'POST'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network response was not ok: ${response.statusText}`);
            }
            return response.json(); // Parse JSON
        })
        .then(data => {
            console.log("Server response:", data);
        })
        .catch(error => {
            console.error("Error:", error);
            setTimeout(() => window.location.href = '/login', 3000);
        });
} */

function requestComments( commentButton, status ){
	
	console.log(`Post Box is shifting to ${status}`);
	let commentBox = commentButton.parentElement.parentElement.querySelector(".postComments.card");
	
	if( status === "open" ){
	
		commentBox.style.display = "block";
		commentButton.setAttribute("onclick", `requestComments(this, 'close')`);
		
	}
	else if( status === "close" ){
		
		commentBox.style.display = "none";
		commentButton.setAttribute("onclick", `requestComments(this, 'open')`);
		
	}
	
	
}

function sendComment( other, articleId ){
	
	const comment = other.parentElement.querySelector(".comment");
	const commentBox = other.parentElement.parentElement.parentElement.querySelector(".postComments > .container");
	
	const authorBox = other.parentElement.parentElement.parentElement.querySelector(".author-box");
	
	let message = comment.value;
	let image = "/uploads" + authorBox.querySelector(".author-profile > img").src.split("/uploads")[1];
	let username = authorBox.querySelector(".author-details > h3").innerHTML;
	let name = authorBox.querySelector(".author-details > span").innerHTML;
	let temp;
	
	console.log( `Name : ${name}, Image : ${image}, Username : ${username}, Message : ${message}  ` )
	
	fetch(`/comment/${articleId}`, {
	        method: 'POST',
			headers: {
	            'Content-Type': 'application/json', // Specify JSON content
	        },
	        body: JSON.stringify({
	            message: comment.value // Use .value to get input or textarea content
	        })
	    })
	        .then(response => {
				
	            if (!response.ok) {
					if( response.status === 400 ){
						temp = "Failed to Comment"
						throw new Error("Bad Request")
					} else if( response.status === 401 ){
						temp = "Inappropriate Comment. Hate Speech Not Allowed."
						throw new Error("Unauthorized")
					} else{
					temp = "Failed to Comment"
						throw new Error(`Network response was not ok: ${response.statusText}`);						
					}
	            }
				temp = "Failed to comment";
	            return response.json(); // Parse JSON
	        })
			.then(data => {
				temp = "Comment Posted Successfully";				
			    console.log("Server response:", data);
			})
			.catch(error => {
	            console.error("Error:", error);
	        })
			.finally( () => {
				comment.placeholder = temp;	
				comment.value = "";
			});
	
}

function sendRequest(other, blogger, action) {
    let meCard = document.querySelector(".card.me");
    let followingsText = meCard.querySelector(".card-body > p.following > strong");
    //  let otherButton = other.querySelector("button");
    let otherIcon = other.querySelector("i");
	let godFather = other.parentElement.parentElement.parentElement;
    let followersText = godFather.querySelector(".card-body > p.follower > strong");

    fetch(`/connection/${action}/${blogger}`, {
        method: 'POST'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network response was not ok: ${response.statusText}`);
            }
            return response.json(); // Parse JSON
        })
        .then(data => {
            console.log("Server response:", data);

            // Update button icon for "other"
            if (action === "follow") {
                otherIcon.classList.remove("bi-person-plus-fill");
                otherIcon.classList.add("bi-person-check-fill");
                other.setAttribute("onclick", `sendRequest(this, ${blogger}, 'unfollow')`);
            } else if (action === "unfollow") {
                otherIcon.classList.remove("bi-person-check-fill");
                otherIcon.classList.add("bi-person-plus-fill");
                other.setAttribute("onclick", `sendRequest(this, ${blogger}, 'follow')`);
            }

            // Increment followings count for "me"
			if (action === "follow") {
			    let currentFollowings = parseInt(followingsText.textContent.split(":")[1].trim());
			    followingsText.textContent = `Followings : ${currentFollowings + 1}`;
				
				currentFollowings = parseInt(followersText.textContent.split(":")[1].trim());
				followersText.textContent = `Followers : ${currentFollowings + 1}`;
			} else if (action === "unfollow") {
			    let currentFollowings = parseInt(followingsText.textContent.split(":")[1].trim());
			    let newFollowings = Math.max(0, currentFollowings - 1); // Ensure it doesn't go below 0
			    followingsText.textContent = `Followings : ${newFollowings}`;
				
				currentFollowings = parseInt(followersText.textContent.split(":")[1].trim());
			    newFollowings = Math.max(0, currentFollowings - 1); // Ensure it doesn't go below 0
			    followersText.textContent = `Followers : ${newFollowings}`;
			}

        })
        .catch(error => {
            console.error("Error:", error);
            setTimeout(() => window.location.href = '/', 3000);
        });
}


function watchPost( id ){
	window.location.href = `/post/${id}`;
}


/* document.querySelectorAll('.oper').forEach(button => {
    button.addEventListener('click', function () {
        // Check if the button is already active
        const isActive = this.classList.contains('operations');
        const isLike = this.classList.contains('like');
        const isDislike = this.classList.contains('dislike');
        const icon = this.querySelector('i');
        let reaction = null;

        // Find like and dislike counters
        const likeButton = this.parentElement.querySelector('.like');
        const dislikeButton = this.parentElement.querySelector('.dislike');
        const likeCount = likeButton.querySelector('span');
        const dislikeCount = dislikeButton.querySelector('span');
        const articleId = this.parentElement.querySelector('.articleid');

        // Initial states
        const likeActive = likeButton.classList.contains('bi-hand-thumbs-up-fill');
        const dislikeActive = dislikeButton.classList.contains('bi-hand-thumbs-down-fill');

        // Toggle logic for like and dislike
        if (isLike) {
            if (likeActive) {
                // Deactivate like
                this.classList.remove('operations');
                icon.classList.remove('bi-hand-thumbs-up-fill');
                icon.classList.add('bi-hand-thumbs-up');
                likeCount.textContent = Math.max(Number(likeCount.textContent) - 1, 0);
                reaction = 'nil';
            } else {
                // Activate like and deactivate dislike if active
                this.classList.add('operations');
                icon.classList.add('bi-hand-thumbs-up-fill');
                icon.classList.remove('bi-hand-thumbs-up');
                likeCount.textContent = Number(likeCount.textContent) + 1;

                if (dislikeActive) {
                    dislikeButton.classList.remove('operations');
                    const dislikeIcon = dislikeButton.querySelector('i');
                    dislikeIcon.classList.remove('bi-hand-thumbs-down-fill');
                    dislikeIcon.classList.add('bi-hand-thumbs-down');
                    dislikeCount.textContent = Math.max(Number(dislikeCount.textContent) - 1, 0);
                }
                reaction = 'like';
            }
            console.log("Like Happened");
        } else if (isDislike) {
            if (dislikeActive) {
                // Deactivate dislike
                this.classList.remove('operations');
                icon.classList.remove('bi-hand-thumbs-down-fill');
                icon.classList.add('bi-hand-thumbs-down');
                dislikeCount.textContent = Math.max(Number(dislikeCount.textContent) - 1, 0);
                reaction = 'nil';
            } else {
                // Activate dislike and deactivate like if active
                this.classList.add('operations');
                icon.classList.add('bi-hand-thumbs-down-fill');
                icon.classList.remove('bi-hand-thumbs-down');
                dislikeCount.textContent = Number(dislikeCount.textContent) + 1;

                if (likeActive) {
                    likeButton.classList.remove('operations');
                    const likeIcon = likeButton.querySelector('i');
                    likeIcon.classList.remove('bi-hand-thumbs-up-fill');
                    likeIcon.classList.add('bi-hand-thumbs-up');
                    likeCount.textContent = Math.max(Number(likeCount.textContent) - 1, 0);
                }
                reaction = 'dislike';
            }
            console.log("Dislike Happened");
        }

        console.log(`Reaction: ${reaction}, Article ID: ${articleId.innerHTML}, Like Count: ${likeCount.textContent}, Dislike Count: ${dislikeCount.textContent}`);

        // Send reaction to server
        fetch(`/reaction/${reaction}/${articleId.innerHTML}`, {
            method: 'POST'
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok: ${response.statusText}`);
                }
                return response.json(); // Parse JSON
            })
            .then(data => {
                console.log("Server response:", data);
            })
            .catch(error => {
                console.error("Error:", error);
                setTimeout(() => window.location.href = '/login', 3000);
            });
    });
}); */

document.querySelectorAll('.oper').forEach(button => {
    button.addEventListener('click', function () {
        // Get button states
        const isLike = this.classList.contains('like');
        const isDislike = this.classList.contains('dislike');
        const icon = this.querySelector('i');
        let reaction = null;

        // Find like and dislike counters
        const likeButton = this.parentElement.querySelector('.like');
        const dislikeButton = this.parentElement.querySelector('.dislike');
        const likeCount = likeButton.querySelector('span');
        const dislikeCount = dislikeButton.querySelector('span');
        const articleId = this.parentElement.querySelector('.articleid');

        // Check if buttons are active
        const likeActive = likeButton.querySelector('i').classList.contains('bi-hand-thumbs-up-fill');
        const dislikeActive = dislikeButton.querySelector('i').classList.contains('bi-hand-thumbs-down-fill');

        if (isLike) {
            if (likeActive) {
                // Deactivate like
                icon.classList.remove('bi-hand-thumbs-up-fill');
                icon.classList.add('bi-hand-thumbs-up');
                likeCount.textContent = Math.max(Number(likeCount.textContent) - 1, 0);
                reaction = 'nil';
            } else {
                // Activate like and deactivate dislike if active
                icon.classList.add('bi-hand-thumbs-up-fill');
                icon.classList.remove('bi-hand-thumbs-up');
                likeCount.textContent = Number(likeCount.textContent) + 1;

                if (dislikeActive) {
                    const dislikeIcon = dislikeButton.querySelector('i');
                    dislikeIcon.classList.remove('bi-hand-thumbs-down-fill');
                    dislikeIcon.classList.add('bi-hand-thumbs-down');
                    dislikeCount.textContent = Math.max(Number(dislikeCount.textContent) - 1, 0);
                }
                reaction = 'like';
            }
        } else if (isDislike) {
            if (dislikeActive) {
                // Deactivate dislike
                icon.classList.remove('bi-hand-thumbs-down-fill');
                icon.classList.add('bi-hand-thumbs-down');
                dislikeCount.textContent = Math.max(Number(dislikeCount.textContent) - 1, 0);
                reaction = 'nil';
            } else {
                // Activate dislike and deactivate like if active
                icon.classList.add('bi-hand-thumbs-down-fill');
                icon.classList.remove('bi-hand-thumbs-down');
                dislikeCount.textContent = Number(dislikeCount.textContent) + 1;

                if (likeActive) {
                    const likeIcon = likeButton.querySelector('i');
                    likeIcon.classList.remove('bi-hand-thumbs-up-fill');
                    likeIcon.classList.add('bi-hand-thumbs-up');
                    likeCount.textContent = Math.max(Number(likeCount.textContent) - 1, 0);
                }
                reaction = 'dislike';
            }
        }

        console.log(`Reaction: ${reaction}, Article ID: ${articleId.innerHTML}, Like Count: ${likeCount.textContent}, Dislike Count: ${dislikeCount.textContent}`);

        // Send reaction to server
        if (reaction !== null) {
            fetch(`/reaction/${reaction}/${articleId.innerHTML}`, {
                method: 'POST'
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Network response was not ok: ${response.statusText}`);
                    }
                    return response.json(); // Parse JSON
                })
                .then(data => {
                    console.log("Server response:", data);
                })
                .catch(error => {
                    console.error("Error:", error);
                    setTimeout(() => window.location.href = '/login', 3000);
                });
        }
    });
});



$(document).ready(function () {

      $('.menu-toggle').click(function () {
        $('.menu-toggle').toggleClass('active');
        $('nav').toggleClass('active');
        $('nav ul').toggleClass('showing');
      });

      $('.posts-wrapper').slick({
        slidesToShow: 3,
        slidesToScroll: 1,
        autoplay: true,
        autoplaySpeed: 2000,
        nextArrow: $('.next'),
        prevArrow: $('.prev'),
        responsive: [{
            breakpoint: 1024,
            settings: {
              slidesToShow: 3,
              slidesToScroll: 3,
              infinite: true,
              dots: false
            }
          },
          {
            breakpoint: 880,
            settings: {
              slidesToShow: 2,
              slidesToScroll: 2,
              infinite: true,
              dots: false
            }
          },
          {
            breakpoint: 480,
            settings: {
              slidesToShow: 1,
              slidesToScroll: 1
            }
          }
        ]
      });
    });

ClassicEditor.create( document.querySelector( '#body' ) )
.then( editor => {
        console.log( editor );
} )
.catch( error => {
        console.error( error );
} );