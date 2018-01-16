package agent.farm;

/** 
 * Person object to go inside farm object. Contains parameters such as age, preferences, memory..
 * @author kellerke
 *
 */
public class Person {

	private int age;
	private int education;
	private int memory;
	private double entrepreneurship;
	
	public Person(int age, int education, int memory, double entrepreneurship) {
		this.age = age;
		this.education = education;
		this.memory = memory;
		this.entrepreneurship = entrepreneurship;
	}
	
	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getEducation() {
		return education;
	}

	public void setEducation(int education) {
		this.education = education;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}

	public double getEntrepreneurship() {
		return entrepreneurship;
	}

	public void setEntrepreneurship(double entrepreneurship2) {
		this.entrepreneurship = entrepreneurship2;
	}

}
