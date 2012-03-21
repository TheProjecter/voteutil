using System;

struct NameVote {
    public string Name;
    public double Rating;
}

interface ElectionMethod {
    /* cast a vote */
    void VoteRatings(NameVote[] ratings);

    /* return results, sorted best to worst, check .Rating == .Rating for ties */
    NameVote[] GetWinners();

    /* Return HTML text which explains how the count worked. */
    string HtmlExplanation();
}

class Hello
{
    static void Main() {
	Console.WriteLine("yo, whirled!");
    }
}
