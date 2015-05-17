# next-build Library

##Build를 통한 Dependency Injection (resources/build.json)
1. Javascript Object사용
2. '#'으로 아이디 지정

##GET
pom.xml에 아래의 레파지토리와 Dependency설정을 추가합니다.

###Repository
    <repository>
        <id>next-mvn-repo</id>
        <url>https://raw.github.com/zerohouse/next/mvn-repo/</url>
    </repository>

###Dependency
	<dependency>
		<groupId>at.begin</groupId>
		<artifactId>next-build</artifactId>
		<version>0.0.1</version>
	</dependency>


### Example
#### 1. build.json
    {
      "right" : {
           "ALLMighty" : "#Users.rootUser", // 다른 오브젝트를 참조시
                                                #으로 아이디 지정
           "newBoard" : "#Users.operators"
      },
      "Users" {
       	  "rootUser" : {
	        	       "email" : "user1@gmail.com",
		               "gender" : "m"
	  	            	}
	       },
           "operators" : [
                "goo@ggo.com" ,
                "goo3@ggo.com" ,
                "goo4@ggo.com" ,
                "goo5@ggo.com" ,
           ]
	}
	
#### 2. build
	@Build("Users.rootUser")  // 자바스크립트처럼 셀렉트
	private User user;
    
    @Build("right")
    @ImplementedBy(AllRight.class) // 인터페이스일 경우 구현체
    private Right right;
    
