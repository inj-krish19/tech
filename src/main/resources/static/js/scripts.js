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


function watchPost( id ){
	window.location.href = `/post/${id}`;
}


document.querySelectorAll('.oper').forEach(button => {
    button.addEventListener('click', function () {
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

        // Toggle logic for like and dislike
        if (isLike) {
            if (isActive) {
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

                if (dislikeButton.classList.contains('operations')) {
                    dislikeButton.classList.remove('operations');
                    const dislikeIcon = dislikeButton.querySelector('i');
                    dislikeIcon.classList.remove('bi-hand-thumbs-down-fill');
                    dislikeIcon.classList.add('bi-hand-thumbs-down');
                    dislikeCount.textContent = Math.max(Number(dislikeCount.textContent) - 1, 0);
                }
				reaction = "like";
            }
			console.log("Like Happened")
        } else if (isDislike) {
            if (isActive) {
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

                if (likeButton.classList.contains('operations')) {
                    likeButton.classList.remove('operations');
                    const likeIcon = likeButton.querySelector('i');
                    likeIcon.classList.remove('bi-hand-thumbs-up-fill');
                    likeIcon.classList.add('bi-hand-thumbs-up');
                    likeCount.textContent = Math.max(Number(likeCount.textContent) - 1, 0);
                }
				reaction = "dislike";
            }
			console.log("DisLike Happened")
        }

        console.log(` Reaction ${reaction} Article ID : ${articleId.innerHTML} Like Count: ${likeCount.textContent}, Dislike Count: ${dislikeCount.textContent}`);
    
		fetch(
			`/reaction/${reaction}/${articleId.innerHTML}`,
			{	method: 'POST',		}
		)
			.then( 	response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok: ${response.statusText}`);
                }
                return response.json(); // Parse JSON
            } )	
			.then( data => { console.log("Data"); } )	
            .catch(error => {
                setTimeout(() => window.location.href = '/login', 3000);
            })
        	.finally();
		
		reaction = null;
			
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